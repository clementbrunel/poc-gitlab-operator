package com.deployment.gitlab.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * Deployment configuration properties
 */
@Configuration
@ConfigurationProperties(prefix = "deployment")
@Data
public class DeploymentConfig {

    private Map<String, String> earPrefix = new HashMap<>();

    /**
     * Gets the ear prefix for a given environment
     */
    public String getEarPrefix(String environment) {
        return earPrefix.getOrDefault(environment, "");
    }
}
