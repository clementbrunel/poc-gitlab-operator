package com.deployment.gitlab.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Model representing a deployment request
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeploymentRequest {

    /**
     * Applications to deploy
     */
    private List<String> applicationNames;

    /**
     * Artifacts (ear files) to deploy, organized by application name
     */
    private Map<String, List<String>> artifacts;

    /**
     * Requester name
     */
    private String requesterName;

    /**
     * Additional comments/notes
     */
    private String notes;

    /**
     * Request date
     */
    private LocalDateTime requestDate;

    /**
     * Target environment (e.g., staging, production)
     */
    private String targetEnvironment;
}
