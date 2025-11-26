package com.deployment.gitlab.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.OffsetDateTime;

/**
 * GitLab Pipeline model
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class GitLabPipeline {

    @JsonProperty("id")
    private Long id;

    @JsonProperty("ref")
    private String ref;

    @JsonProperty("status")
    private String status;

    @JsonProperty("created_at")
    private OffsetDateTime createdAt;

    @JsonProperty("updated_at")
    private OffsetDateTime updatedAt;

    @JsonProperty("web_url")
    private String webUrl;
}
