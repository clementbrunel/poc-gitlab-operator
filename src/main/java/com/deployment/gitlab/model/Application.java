package com.deployment.gitlab.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Model representing an application to be deployed
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Application {

    /**
     * Application name
     */
    private String name;

    /**
     * Application description
     */
    private String description;

    /**
     * GitLab project ID (numeric)
     */
    private Integer gitlabProjectId;

    /**
     * GitLab project path (namespace/project-name)
     */
    private String gitlabProjectPath;

    /**
     * Branch to monitor (typically "develop")
     */
    private String branch;

    /**
     * Application enabled
     */
    private boolean enabled;

    /**
     * Can be frozen during code freeze
     */
    private boolean freezable;
}
