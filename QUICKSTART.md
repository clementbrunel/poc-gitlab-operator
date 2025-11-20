# Quick Start Guide

## Application Overview

GitLab Deployment Manager is a Spring Boot application that helps manage deployments in coordination with GitLab Enterprise.

## Demo Mode (No Configuration Required)

The application can start in DEMO mode without any environment variables configured. This allows you to validate the architecture before functional testing.

### Start in Demo Mode

```bash
# 1. Compile the application
mvn clean package

# 2. Run the application (no env vars needed)
./start.sh
# OR
java -jar target/gitlab-deployment-manager-1.0.0-SNAPSHOT.jar
```

The application will:
- Start on http://localhost:8080
- Use default admin credentials: `admin` / `admin123`
- Display warnings about missing configuration
- Work with limited functionality (UI navigation works, but GitLab/Email features are simulated)

### What Works in Demo Mode

- ✓ Web interface and navigation
- ✓ Login to admin panel
- ✓ View all pages and forms
- ✓ Code freeze management (in-memory)
- ✓ Deployment request form (logs email to console instead of sending)

### What Requires Configuration

- GitLab integration (requires GITLAB_URL and GITLAB_TOKEN)
- Email sending (requires SMTP configuration)
- Real application monitoring

## Production Mode (Full Configuration)

### 1. Configure Environment Variables

```bash
cp .env.example .env
# Edit .env with your actual values
```

Required variables:
```bash
GITLAB_URL=https://your-gitlab.com
GITLAB_TOKEN=glpat-your-token
SMTP_HOST=smtp.company.com
SMTP_USERNAME=user@company.com
SMTP_PASSWORD=your-smtp-password
DEPLOYMENT_EMAIL=deploy-team@company.com
```

### 2. Configure Applications

Edit `src/main/resources/applications.yaml` with your real GitLab projects:

```yaml
applications:
  - name: "Frontend App"
    description: "Main web application"
    gitlabProjectId: 123
    gitlabProjectPath: "team/frontend"
    branch: "develop"
    enabled: true
    freezable: true
```

### 3. Compile and Run

```bash
mvn clean package
./start.sh
```

Check logs for configuration validation:
```bash
tail -f logs/application.log
```

### 4. Access the Application

- **Home**: http://localhost:8080
- **Versions**: http://localhost:8080/versions
- **Deployment**: http://localhost:8080/deployment
- **Admin**: http://localhost:8080/admin/login

## Architecture Validation

### Project Structure

```
poc-gitlab-operator/
├── src/main/java/com/deployment/gitlab/
│   ├── DeploymentManagerApplication.java   # Main application
│   ├── config/
│   │   ├── GitLabConfig.java               # GitLab API configuration
│   │   ├── GitLabClient.java               # GitLab API client
│   │   ├── SecurityConfig.java             # Spring Security
│   │   └── ConfigurationChecker.java       # Startup validation
│   ├── controller/
│   │   ├── HomeController.java             # Home page
│   │   ├── VersionsController.java         # Versions page
│   │   ├── DeploymentController.java       # Deployment form
│   │   └── AdminController.java            # Admin panel
│   ├── model/
│   │   ├── Application.java                # App model
│   │   ├── ApplicationVersion.java         # Version info
│   │   ├── DeploymentRequest.java          # Deployment request
│   │   ├── CodeFreeze.java                 # Code freeze state
│   │   └── GitLab*.java                    # GitLab DTOs
│   ├── repository/
│   │   └── ApplicationRepository.java      # In-memory app repo
│   └── service/
│       ├── GitLabService.java              # GitLab integration
│       ├── CodeFreezeService.java          # Freeze management
│       ├── DeploymentService.java          # Deployment logic
│       └── EmailService.java               # Email sending
├── src/main/resources/
│   ├── application.yml                     # Spring Boot config
│   ├── applications.yaml                   # App list (EDIT THIS)
│   ├── gitlab-openapi-v2.yaml             # GitLab 18.4.1 API spec
│   ├── templates/                          # Thymeleaf templates
│   └── static/css/style.css               # Styles
├── .env.example                            # Example env vars
├── start.sh                                # Start script
├── stop.sh                                 # Stop script
├── README.md                               # Full documentation
└── DEPLOYMENT.md                           # VM deployment guide
```

### Key Features

1. **No Database**: All data in memory (perfect for simple use cases)
2. **Single Admin**: Configured via environment variables
3. **GitLab API v4**: Uses GitLab 18.4.1-ee API specification
4. **Email Notifications**: Send deployment requests by email
5. **Code Freeze**: Admin can block deployments during test periods

### Technologies

- Spring Boot 3.2.1
- Spring Security 6
- Thymeleaf
- Spring Mail
- WebClient (reactive)
- Java 17

## Troubleshooting

### Application won't start

Check:
1. Java 17+ installed: `java -version`
2. JAR exists: `ls -l target/gitlab-deployment-manager-*.jar`
3. Port 8080 available: `lsof -i :8080`

### Maven build fails

```bash
# Clear Maven cache and retry
rm -rf ~/.m2/repository
mvn clean package
```

### Cannot access admin panel

Default credentials (change ADMIN_USERNAME/ADMIN_PASSWORD in production):
- Username: `admin`
- Password: `admin123`

## Next Steps

1. **Test Demo Mode**: Start the app and explore the UI
2. **Configure GitLab**: Add your GitLab URL and token
3. **Configure Apps**: Edit applications.yaml with real projects
4. **Test Integration**: Verify versions page shows real data
5. **Deploy to VM**: Follow DEPLOYMENT.md for production setup

## Support

For detailed documentation:
- Full README: [README.md](README.md)
- Deployment guide: [DEPLOYMENT.md](DEPLOYMENT.md)
- GitLab API spec: `src/main/resources/gitlab-openapi-v2.yaml`
