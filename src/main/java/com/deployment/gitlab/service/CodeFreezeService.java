package com.deployment.gitlab.service;

import com.deployment.gitlab.model.Application;
import com.deployment.gitlab.model.CodeFreeze;
import com.deployment.gitlab.repository.ApplicationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service to manage code freeze
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CodeFreezeService {

    private final ApplicationRepository applicationRepository;

    // Code freeze state in memory
    private CodeFreeze codeFreeze = CodeFreeze.builder()
            .globalFreezeEnabled(false)
            .build();

    /**
     * Retrieves the current code freeze status
     */
    public CodeFreeze getCodeFreezeStatus() {
        return codeFreeze;
    }

    /**
     * Enables global code freeze
     */
    public void enableGlobalFreeze(String reason, LocalDateTime endDate, String activatedBy) {
        log.info("Enabling global code freeze by {}", activatedBy);
        codeFreeze.setGlobalFreezeEnabled(true);
        codeFreeze.setReason(reason);
        codeFreeze.setFreezeStartDate(LocalDateTime.now());
        codeFreeze.setFreezeEndDate(endDate);
        codeFreeze.setActivatedBy(activatedBy);
    }

    /**
     * Disables global code freeze
     */
    public void disableGlobalFreeze() {
        log.info("Disabling global code freeze");
        codeFreeze.setGlobalFreezeEnabled(false);
        codeFreeze.setReason(null);
        codeFreeze.setFreezeStartDate(null);
        codeFreeze.setFreezeEndDate(null);
    }

    /**
     * Freezes a specific application
     */
    public void freezeApplication(String applicationName, String reason, String activatedBy) {
        Application app = applicationRepository.findByName(applicationName).orElse(null);
        if (app != null && app.isFreezable()) {
            log.info("Freezing application {} by {}", applicationName, activatedBy);
            codeFreeze.freezeApplication(applicationName);
        } else {
            log.warn("Unable to freeze application {} (not found or not freezable)", applicationName);
        }
    }

    /**
     * Unfreezes a specific application
     */
    public void unfreezeApplication(String applicationName) {
        log.info("Unfreezing application {}", applicationName);
        codeFreeze.unfreezeApplication(applicationName);
    }

    /**
     * Checks if an application is frozen
     */
    public boolean isApplicationFrozen(String applicationName) {
        Application app = applicationRepository.findByName(applicationName).orElse(null);
        if (app == null || !app.isFreezable()) {
            return false;
        }
        return codeFreeze.isApplicationFrozen(applicationName);
    }

    /**
     * Checks if an application deployment is allowed
     */
    public boolean isDeploymentAllowed(String applicationName) {
        return !isApplicationFrozen(applicationName);
    }

    /**
     * Retrieves all freezable applications
     */
    public List<Application> getAllFreezableApplications() {
        return applicationRepository.findAllFreezable();
    }
}
