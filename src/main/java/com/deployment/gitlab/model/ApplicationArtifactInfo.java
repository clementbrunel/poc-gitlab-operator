package com.deployment.gitlab.model;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Information about application artifacts (.ear files)
 */
@Data
@Builder
public class ApplicationArtifactInfo {

    /**
     * Application name
     */
    private String applicationName;

    /**
     * List of .ear artifact files found
     */
    private List<String> artifacts;

    /**
     * Build date from the job
     */
    private LocalDateTime buildDate;

    /**
     * URL to the GitLab job
     */
    private String jobUrl;

    /**
     * Whether the artifact retrieval was successful
     */
    private boolean success;

    /**
     * Error message if retrieval failed
     */
    private String errorMessage;
}
