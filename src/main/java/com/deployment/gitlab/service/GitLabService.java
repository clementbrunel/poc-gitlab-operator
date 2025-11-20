package com.deployment.gitlab.service;

import com.deployment.gitlab.config.GitLabClient;
import com.deployment.gitlab.model.*;
import com.deployment.gitlab.repository.ApplicationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Service to interact with GitLab
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GitLabService {

    private final GitLabClient gitLabClient;
    private final ApplicationRepository applicationRepository;
    private final CodeFreezeService codeFreezeService;

    /**
     * Retrieves versions of all active applications
     */
    public List<ApplicationVersion> getAllApplicationVersions() {
        List<Application> applications = applicationRepository.findAllEnabled();
        List<ApplicationVersion> versions = new ArrayList<>();

        for (Application app : applications) {
            ApplicationVersion version = getApplicationVersion(app);
            if (version != null) {
                versions.add(version);
            }
        }

        return versions;
    }

    /**
     * Retrieves the version of a specific application
     */
    public ApplicationVersion getApplicationVersion(Application app) {
        try {
            log.debug("Retrieving version for application: {}", app.getName());

            // Retrieves the last commit of the branch
            GitLabCommit commit = gitLabClient.getLastCommit(
                    app.getGitlabProjectId(),
                    app.getBranch()
            );

            if (commit == null) {
                log.warn("Unable to retrieve commit for application: {}", app.getName());
                return null;
            }

            // Builds the version
            ApplicationVersion version = ApplicationVersion.builder()
                    .application(app)
                    .version(extractVersion(commit))
                    .commitSha(commit.getShortId())
                    .commitMessage(commit.getTitle())
                    .commitAuthor(commit.getAuthorName())
                    .commitDate(commit.getCommittedDate())
                    .commitUrl(commit.getWebUrl())
                    .frozen(codeFreezeService.isApplicationFrozen(app.getName()))
                    .build();

            return version;
        } catch (Exception e) {
            log.error("Error retrieving version for {}: {}",
                    app.getName(), e.getMessage());
            return null;
        }
    }

    /**
     * Extracts a version from a commit
     * Searches for tags or uses the short SHA
     */
    private String extractVersion(GitLabCommit commit) {
        // For now, we use the short SHA
        // TODO: Improve to extract tags if available
        return commit.getShortId();
    }

    /**
     * Tests the GitLab connection
     */
    public boolean testGitLabConnection() {
        return gitLabClient.testConnection();
    }
}
