package com.deployment.gitlab.config;

import com.deployment.gitlab.model.GitLabBranch;
import com.deployment.gitlab.model.GitLabCommit;
import com.deployment.gitlab.model.GitLabProject;
import com.deployment.gitlab.model.GitLabPipeline;
import com.deployment.gitlab.model.GitLabJob;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

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
     * Retrieves pipelines for a project on a specific branch with a specific status
     */
    public List<GitLabPipeline> getPipelines(Integer projectId, String branchName, String status) {
        log.debug("Retrieving pipelines for project {} on branch {} with status {}", projectId, branchName, status);
        try {
            return gitLabWebClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/projects/{id}/pipelines")
                            .queryParam("ref", branchName)
                            .queryParam("status", status)
                            .queryParam("per_page", 20)
                            .queryParam("order_by", "updated_at")
                            .queryParam("sort", "desc")
                            .build(projectId))
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<List<GitLabPipeline>>() {})
                    .timeout(Duration.ofMillis(gitLabConfig.getTimeout()))
                    .block();
        } catch (WebClientResponseException e) {
            log.error("Error retrieving pipelines for project {}: {} - {}",
                    projectId, e.getStatusCode(), e.getMessage());
            return List.of();
        } catch (Exception e) {
            log.error("Error retrieving pipelines for project {}: {}", projectId, e.getMessage());
            return List.of();
        }
    }

    /**
     * Retrieves jobs for a specific pipeline
     */
    public List<GitLabJob> getJobs(Integer projectId, Long pipelineId) {
        log.debug("Retrieving jobs for pipeline {} in project {}", pipelineId, projectId);
        try {
            return gitLabWebClient.get()
                    .uri("/projects/{id}/pipelines/{pipelineId}/jobs", projectId, pipelineId)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<List<GitLabJob>>() {})
                    .timeout(Duration.ofMillis(gitLabConfig.getTimeout()))
                    .block();
        } catch (WebClientResponseException e) {
            log.error("Error retrieving jobs for pipeline {}: {} - {}",
                    pipelineId, e.getStatusCode(), e.getMessage());
            return List.of();
        } catch (Exception e) {
            log.error("Error retrieving jobs for pipeline {}: {}", pipelineId, e.getMessage());
            return List.of();
        }
    }

    /**
     * Retrieves artifact file paths (.ear files) for a specific job
     */
    public List<String> getJobArtifactFiles(Integer projectId, Long jobId) {
        log.debug("Retrieving artifact files for job {} in project {}", jobId, projectId);
        try {
            // GitLab API: GET /projects/:id/jobs/:job_id/artifacts
            // Returns a ZIP file or we can use /projects/:id/jobs/:job_id/artifacts/raw/*path
            // But to list artifacts, we need: GET /projects/:id/jobs/:job_id/artifacts (returns JSON tree)

            JsonNode artifacts = gitLabWebClient.get()
                    .uri("/projects/{id}/jobs/{jobId}/artifacts", projectId, jobId)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .timeout(Duration.ofMillis(gitLabConfig.getTimeout()))
                    .block();

            List<String> earFiles = new ArrayList<>();

            if (artifacts != null && artifacts.isArray()) {
                for (JsonNode artifact : artifacts) {
                    String filename = artifact.has("filename") ? artifact.get("filename").asText() : "";
                    if (filename.toLowerCase().endsWith(".ear")) {
                        earFiles.add(filename);
                    }
                }
            }

            log.debug("Found {} .ear artifact(s) for job {}", earFiles.size(), jobId);
            return earFiles;

        } catch (WebClientResponseException.NotFound e) {
            log.debug("No artifacts found for job {}", jobId);
            return List.of();
        } catch (WebClientResponseException e) {
            log.error("Error retrieving artifacts for job {}: {} - {}",
                    jobId, e.getStatusCode(), e.getMessage());
            return List.of();
        } catch (Exception e) {
            log.error("Error retrieving artifacts for job {}: {}", jobId, e.getMessage());
            return List.of();
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
