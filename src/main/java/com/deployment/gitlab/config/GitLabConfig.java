package com.deployment.gitlab.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Configuration for GitLab integration
 */
@Configuration
@ConfigurationProperties(prefix = "gitlab")
@Data
public class GitLabConfig {

    /**
     * GitLab instance URL
     */
    private String url;

    /**
     * GitLab authentication token
     */
    private String token;

    /**
     * API version (v4 by default)
     */
    private String apiVersion = "v4";

    /**
     * Request timeout in milliseconds
     */
    private int timeout = 30000;

    /**
     * Name of the GitLab job containing the logs with .ear files list
     */
    private String artifactJobName = "jobftp";

    /**
     * Creates a WebClient configured for GitLab
     */
    @Bean
    public WebClient gitLabWebClient() {
        WebClient.Builder builder = WebClient.builder()
                .baseUrl(url + "/api/" + apiVersion)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);

        // Add token only if it is configured
        if (token != null && !token.isEmpty()) {
            builder.defaultHeader("PRIVATE-TOKEN", token);
        }

        return builder.build();
    }

    /**
     * Checks if the GitLab configuration is complete
     */
    public boolean isConfigured() {
        return token != null && !token.isEmpty() &&
               url != null && !url.isEmpty() && !url.equals("https://gitlab.com");
    }
}
