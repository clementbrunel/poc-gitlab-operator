package com.deployment.gitlab.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.OffsetDateTime;

/**
 * GitLab Job model
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class GitLabJob {

    @JsonProperty("id")
    private Long id;

    @JsonProperty("name")
    private String name;

    @JsonProperty("status")
    private String status;

    @JsonProperty("created_at")
    private OffsetDateTime createdAt;

    @JsonProperty("started_at")
    private OffsetDateTime startedAt;

    @JsonProperty("finished_at")
    private OffsetDateTime finishedAt;

    @JsonProperty("web_url")
    private String webUrl;

    @JsonProperty("pipeline")
    private GitLabPipelineRef pipeline;

    /**
     * Nested pipeline reference in job
     */
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class GitLabPipelineRef {
        @JsonProperty("id")
        private Long id;
    }
}
