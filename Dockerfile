# Multi-stage Dockerfile for GitLab Deployment Manager
# Stage 1: Build application with Maven
FROM maven:3.9-eclipse-temurin-21-alpine AS builder

WORKDIR /app

# Copy pom.xml and download dependencies (cache layer)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code and build
COPY src ./src
RUN mvn clean package -DskipTests -B

# Stage 2: Runtime with Oracle OpenJDK 21
FROM container-registry.oracle.com/java/jdk:21-oraclelinux8

WORKDIR /app

# Create application user for security
RUN useradd -m -u 1001 appuser && \
    chown -R appuser:appuser /app

# Copy JAR from builder stage
COPY --from=builder /app/target/gitlab-deployment-manager-*.jar /app/application.jar

# Copy application.yml (optional, can be overridden with volume mount)
COPY --from=builder /app/src/main/resources/application.yml /app/config/application.yml

# Expose port (default Spring Boot port)
EXPOSE 8080

# Switch to non-root user
USER appuser

# Environment variables (can be overridden at runtime)
ENV SPRING_CONFIG_LOCATION=file:/app/config/application.yml
ENV JAVA_OPTS="-Xms256m -Xmx512m"

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

# Run application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/application.jar"]
