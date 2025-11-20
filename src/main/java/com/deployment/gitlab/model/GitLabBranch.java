package com.deployment.gitlab.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Model representing a GitLab branch (DTO)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class GitLabBranch {

    @JsonProperty("name")
    private String name;

    @JsonProperty("merged")
    private boolean merged;

    @JsonProperty("protected")
    private boolean protectedBranch;

    @JsonProperty("default")
    private boolean defaultBranch;

    @JsonProperty("commit")
    private GitLabCommit commit;

    @JsonProperty("web_url")
    private String webUrl;
}
