package com.deployment.gitlab.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * Model representing a GitLab commit (DTO)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class GitLabCommit {

    @JsonProperty("id")
    private String id;

    @JsonProperty("short_id")
    private String shortId;

    @JsonProperty("title")
    private String title;

    @JsonProperty("message")
    private String message;

    @JsonProperty("author_name")
    private String authorName;

    @JsonProperty("author_email")
    private String authorEmail;

    @JsonProperty("created_at")
    private OffsetDateTime createdAt;

    @JsonProperty("committed_date")
    private OffsetDateTime committedDate;

    @JsonProperty("web_url")
    private String webUrl;
}
