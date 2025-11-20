package com.deployment.gitlab.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

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
     * Requester name
     */
    private String requesterName;

    /**
     * Requester email
     */
    private String requesterEmail;

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
