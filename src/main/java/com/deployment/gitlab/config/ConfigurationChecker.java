package com.deployment.gitlab.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Checks configuration at startup and displays warnings
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ConfigurationChecker {

    private final GitLabConfig gitLabConfig;

    @Value("${spring.mail.host:unconfigured.smtp}")
    private String smtpHost;

    @Value("${deployment.email.to}")
    private String deploymentEmail;

    @EventListener(ApplicationReadyEvent.class)
    public void checkConfiguration() {
        log.info("=".repeat(80));
        log.info("CONFIGURATION CHECK");
        log.info("=".repeat(80));

        boolean fullyConfigured = true;

        // GitLab verification
        if (!gitLabConfig.isConfigured()) {
            log.warn("⚠️  GitLab configuration INCOMPLETE");
            log.warn("   - GITLAB_URL: {}", gitLabConfig.getUrl());
            log.warn("   - GITLAB_TOKEN: {}", gitLabConfig.getToken() != null && !gitLabConfig.getToken().isEmpty() ? "***configured***" : "NOT CONFIGURED");
            log.warn("   → GitLab features will not be available");
            fullyConfigured = false;
        } else {
            log.info("✓ GitLab configuration: OK");
            log.info("   - URL: {}", gitLabConfig.getUrl());
        }

        // SMTP verification
        if (smtpHost == null || smtpHost.isEmpty() || "unconfigured.smtp".equals(smtpHost)) {
            log.warn("⚠️  SMTP configuration INCOMPLETE");
            log.warn("   - SMTP_HOST: NOT CONFIGURED");
            log.warn("   → Email sending will not work (demo mode)");
            fullyConfigured = false;
        } else {
            log.info("✓ SMTP configuration: OK");
            log.info("   - Host: {}", smtpHost);
        }

        // Deployment email verification
        if (deploymentEmail.equals("deploy-team@example.com")) {
            log.warn("⚠️  Default deployment email");
            log.warn("   - DEPLOYMENT_EMAIL: {}", deploymentEmail);
            log.warn("   → Configure DEPLOYMENT_EMAIL with the team's actual email");
            fullyConfigured = false;
        } else {
            log.info("✓ Deployment email: {}", deploymentEmail);
        }

        log.info("=".repeat(80));

        if (fullyConfigured) {
            log.info("✓ Configuration COMPLETE - Application ready for production");
        } else {
            log.warn("⚠️  Configuration INCOMPLETE - DEMO/DEVELOPMENT mode");
            log.warn("   Application is starting but some features will be limited.");
            log.warn("   For complete configuration, set the environment variables:");
            log.warn("   - GITLAB_URL, GITLAB_TOKEN");
            log.warn("   - SMTP_HOST (and optionally SMTP_USERNAME/SMTP_PASSWORD if auth is required)");
            log.warn("   - DEPLOYMENT_EMAIL");
        }

        log.info("=".repeat(80));
        log.info("Application available at: http://localhost:{}", gitLabConfig.getUrl().contains("8080") ? "8080" : "see SERVER_PORT");
        log.info("Admin interface: /admin/login (user: see ADMIN_USERNAME/ADMIN_PASSWORD)");
        log.info("=".repeat(80));
    }
}
