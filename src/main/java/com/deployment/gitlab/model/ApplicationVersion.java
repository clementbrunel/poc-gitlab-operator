package com.deployment.gitlab.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Model representing the version of an application on a branch
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationVersion {

    /**
     * Associated application
     */
    private Application application;

    /**
     * Current version (tag, commit SHA, or extracted version)
     */
    private String version;

    /**
     * Last commit SHA
     */
    private String commitSha;

    /**
     * Last commit message
     */
    private String commitMessage;

    /**
     * Last commit author
     */
    private String commitAuthor;

    /**
     * Last commit date
     */
    private LocalDateTime commitDate;

    /**
     * URL to the commit in GitLab
     */
    private String commitUrl;

    /**
     * Application currently frozen (code freeze)
     */
    private boolean frozen;
}
