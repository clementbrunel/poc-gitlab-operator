package com.deployment.gitlab.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Model representing the code freeze state
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CodeFreeze {

    /**
     * Global code freeze enabled
     */
    private boolean globalFreezeEnabled;

    /**
     * Specifically frozen applications (application name)
     */
    @Builder.Default
    private Set<String> frozenApplications = new HashSet<>();

    /**
     * Freeze reason
     */
    private String reason;

    /**
     * Freeze activation date
     */
    private LocalDateTime freezeStartDate;

    /**
     * Expected freeze end date
     */
    private LocalDateTime freezeEndDate;

    /**
     * User who activated the freeze
     */
    private String activatedBy;

    /**
     * Checks if an application is frozen
     */
    public boolean isApplicationFrozen(String applicationName) {
        return globalFreezeEnabled || frozenApplications.contains(applicationName);
    }

    /**
     * Freezes an application
     */
    public void freezeApplication(String applicationName) {
        frozenApplications.add(applicationName);
    }

    /**
     * Unfreezes an application
     */
    public void unfreezeApplication(String applicationName) {
        frozenApplications.remove(applicationName);
    }
}
