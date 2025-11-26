package com.deployment.gitlab.service;

import com.deployment.gitlab.model.Application;
import com.deployment.gitlab.model.DeploymentRequest;
import com.deployment.gitlab.repository.ApplicationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for managing deployments
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DeploymentService {

    private final EmailService emailService;
    private final CodeFreezeService codeFreezeService;
    private final ApplicationRepository applicationRepository;

    /**
     * Submits a deployment request
     */
    public void submitDeploymentRequest(DeploymentRequest request) {
        log.info("Submitting deployment request by {}", request.getRequesterName());

        // Validation: check that applications are not frozen
        List<String> frozenApps = new ArrayList<>();
        for (String appName : request.getApplicationNames()) {
            if (codeFreezeService.isApplicationFrozen(appName)) {
                frozenApps.add(appName);
            }
        }

        if (!frozenApps.isEmpty()) {
            String message = "Les applications suivantes sont actuellement gelées : " +
                    String.join(", ", frozenApps);
            log.warn("Attempted deployment of frozen applications: {}", frozenApps);
            throw new IllegalStateException(message);
        }

        // Validation: check that all applications exist
        for (String appName : request.getApplicationNames()) {
            Application app = applicationRepository.findByName(appName).orElse(null);
            if (app == null) {
                throw new IllegalArgumentException("Unknown application: " + appName);
            }
        }

        // Add request date
        request.setRequestDate(LocalDateTime.now());

        // Send email
        emailService.sendDeploymentRequest(request);

        log.info("Deployment request sent successfully for {} application(s)",
                request.getApplicationNames().size());
    }

    /**
     * Checks if a list of applications can be deployed
     */
    public List<String> checkDeploymentEligibility(List<String> applicationNames) {
        List<String> issues = new ArrayList<>();

        for (String appName : applicationNames) {
            // Check that the application exists
            Application app = applicationRepository.findByName(appName).orElse(null);
            if (app == null) {
                issues.add("Application '" + appName + "' does not exist");
                continue;
            }

            // Check that the application is not frozen
            if (codeFreezeService.isApplicationFrozen(appName)) {
                issues.add("Application '" + appName + "' est actuellement gelée");
            }
        }

        return issues;
    }
}
