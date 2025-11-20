package com.deployment.gitlab.config;

import com.deployment.gitlab.model.GitLabBranch;
import com.deployment.gitlab.model.GitLabCommit;
import com.deployment.gitlab.model.GitLabProject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * Client to interact with the GitLab API
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class GitLabClient {

    private final WebClient gitLabWebClient;
    private final GitLabConfig gitLabConfig;

    /**
     * Retrieves a GitLab project by its ID
     */
    public GitLabProject getProject(Integer projectId) {
        log.debug("Retrieving GitLab project with ID: {}", projectId);
        try {
            return gitLabWebClient.get()
                    .uri("/projects/{id}", projectId)
                    .retrieve()
                    .bodyToMono(GitLabProject.class)
                    .timeout(Duration.ofMillis(gitLabConfig.getTimeout()))
                    .block();
        } catch (WebClientResponseException e) {
            log.error("Error retrieving project {}: {} - {}", projectId, e.getStatusCode(), e.getMessage());
            return null;
        } catch (Exception e) {
            log.error("Error retrieving project {}: {}", projectId, e.getMessage());
            return null;
        }
    }

    /**
     * Retrieves a specific branch of a project
     */
    public GitLabBranch getBranch(Integer projectId, String branchName) {
        log.debug("Retrieving branch {} of project {}", branchName, projectId);
        try {
            return gitLabWebClient.get()
                    .uri("/projects/{id}/repository/branches/{branch}", projectId, branchName)
                    .retrieve()
                    .bodyToMono(GitLabBranch.class)
                    .timeout(Duration.ofMillis(gitLabConfig.getTimeout()))
                    .block();
        } catch (WebClientResponseException e) {
            log.error("Error retrieving branch {} of project {}: {} - {}",
                    branchName, projectId, e.getStatusCode(), e.getMessage());
            return null;
        } catch (Exception e) {
            log.error("Error retrieving branch {} of project {}: {}",
                    branchName, projectId, e.getMessage());
            return null;
        }
    }

    /**
     * Retrieves the last commit of a branch
     */
    public GitLabCommit getLastCommit(Integer projectId, String branchName) {
        log.debug("Retrieving last commit of branch {} of project {}", branchName, projectId);
        try {
            // Retrieves the branch which contains the last commit
            GitLabBranch branch = getBranch(projectId, branchName);
            if (branch != null && branch.getCommit() != null) {
                return branch.getCommit();
            }
            return null;
        } catch (Exception e) {
            log.error("Error retrieving last commit of branch {} of project {}: {}",
                    branchName, projectId, e.getMessage());
            return null;
        }
    }

    /**
     * Retrieves a specific commit by its SHA
     */
    public GitLabCommit getCommit(Integer projectId, String commitSha) {
        log.debug("Retrieving commit {} of project {}", commitSha, projectId);
        try {
            return gitLabWebClient.get()
                    .uri("/projects/{id}/repository/commits/{sha}", projectId, commitSha)
                    .retrieve()
                    .bodyToMono(GitLabCommit.class)
                    .timeout(Duration.ofMillis(gitLabConfig.getTimeout()))
                    .block();
        } catch (WebClientResponseException e) {
            log.error("Error retrieving commit {} of project {}: {} - {}",
                    commitSha, projectId, e.getStatusCode(), e.getMessage());
            return null;
        } catch (Exception e) {
            log.error("Error retrieving commit {} of project {}: {}",
                    commitSha, projectId, e.getMessage());
            return null;
        }
    }

    /**
     * GitLab connection test
     */
    public boolean testConnection() {
        log.info("Testing GitLab connection: {}", gitLabConfig.getUrl());
        try {
            gitLabWebClient.get()
                    .uri("/version")
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofMillis(gitLabConfig.getTimeout()))
                    .block();
            log.info("GitLab connection successful");
            return true;
        } catch (Exception e) {
            log.error("GitLab connection failed: {}", e.getMessage());
            return false;
        }
    }
}
