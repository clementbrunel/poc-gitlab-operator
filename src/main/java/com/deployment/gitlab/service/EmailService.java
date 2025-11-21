package com.deployment.gitlab.service;

import com.deployment.gitlab.model.DeploymentRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * Service for sending emails
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${deployment.email.to}")
    private String deploymentEmail;

    @Value("${deployment.email.from}")
    private String fromEmail;

    @Value("#{${deployment.email.subjects}}")
    private Map<String, String> emailSubjects;

    @Value("${SMTP_USERNAME:}")
    private String smtpUsername;

    /**
     * Sends a deployment request email
     */
    public void sendDeploymentRequest(DeploymentRequest request) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

        String emailSubject = emailSubjects.getOrDefault(
            request.getTargetEnvironment(),
            "[DÉPLOIEMENT] Nouvelle demande de déploiement"
        );

        // Log deployment request details
        log.info("=================================================");
        log.info("📧 Processing deployment request");
        log.info("=================================================");
        log.info("Requester: {}", request.getRequesterName());
        log.info("Target Environment: {}", request.getTargetEnvironment());
        log.info("Request Date: {}", request.getRequestDate().format(formatter));
        log.info("Number of Applications: {}", request.getApplicationNames().size());
        log.info("Applications to deploy:");
        for (String appName : request.getApplicationNames()) {
            log.info("  • {}", appName);
        }
        if (request.getNotes() != null && !request.getNotes().isEmpty()) {
            log.info("Comments: {}", request.getNotes());
        } else {
            log.info("Comments: (none)");
        }
        log.info("=================================================");

        // Demo mode if SMTP is not configured
        if (smtpUsername == null || smtpUsername.isEmpty()) {
            log.warn("⚠️  DEMO mode - SMTP not configured");
            log.info("Email subject: {}", emailSubject);
            log.info("=== EMAIL BODY ===");
            log.info(buildEmailBody(request));
            log.info("=== END EMAIL BODY ===");
            log.info("💡 Configure SMTP_USERNAME, SMTP_PASSWORD and SMTP_HOST to send real emails");
            return;
        }

        try {
            log.info("📤 Sending deployment request email...");

            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(deploymentEmail);
            message.setCc(request.getRequesterEmail());
            message.setSubject(emailSubject);
            message.setText(buildEmailBody(request));

            mailSender.send(message);

            log.info("✅ Deployment email sent successfully");
            log.info("=================================================");
        } catch (Exception e) {
            log.error("❌ Error sending deployment email", e);
            log.info("=================================================");
            throw new RuntimeException("Email sending failed: " + e.getMessage());
        }
    }

    /**
     * Loads email template for the specified environment
     */
    private String loadEmailTemplate(String environment) {
        try {
            String templatePath = "email-templates/deployment-" + environment + ".txt";
            ClassPathResource resource = new ClassPathResource(templatePath);

            if (!resource.exists()) {
                log.warn("Template not found: {}, using fallback", templatePath);
                return loadFallbackTemplate();
            }

            return new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.error("Error loading email template for environment {}: {}", environment, e.getMessage());
            return loadFallbackTemplate();
        }
    }

    /**
     * Fallback template if environment-specific template is not found
     */
    private String loadFallbackTemplate() {
        return """
                Demande de Déploiement
                =====================================================

                Bonjour,

                Une nouvelle demande de déploiement a été soumise.

                INFORMATIONS DE LA DEMANDE
                ---------------------------
                Environnement cible : {{TARGET_ENVIRONMENT}}
                Date de la demande : {{REQUEST_DATE}}
                Demandeur : {{REQUESTER_NAME}}

                APPLICATIONS À DÉPLOYER
                ------------------------
                {{APPLICATIONS}}

                COMMENTAIRES / NOTES
                --------------------
                {{COMMENT}}

                --
                Cordialement,
                {{REQUESTER_NAME}}

                ---
                Ce message a été généré automatiquement par GitLab Deployment Manager
                """;
    }

    /**
     * Builds the email body from template
     */
    private String buildEmailBody(DeploymentRequest request) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

        // Load template for the target environment
        String template = loadEmailTemplate(request.getTargetEnvironment());

        // Build applications list
        StringBuilder applicationsList = new StringBuilder();
        for (String appName : request.getApplicationNames()) {
            applicationsList.append("  • ").append(appName).append("\n");
        }

        // Build comment section
        String comment = (request.getNotes() != null && !request.getNotes().isEmpty())
            ? request.getNotes()
            : "(Aucun commentaire)";

        // Replace placeholders
        String emailBody = template
            .replace("{{TARGET_ENVIRONMENT}}", request.getTargetEnvironment())
            .replace("{{REQUEST_DATE}}", request.getRequestDate().format(formatter))
            .replace("{{REQUESTER_NAME}}", request.getRequesterName())
            .replace("{{APPLICATIONS}}", applicationsList.toString().trim())
            .replace("{{COMMENT}}", comment);

        return emailBody;
    }
}
