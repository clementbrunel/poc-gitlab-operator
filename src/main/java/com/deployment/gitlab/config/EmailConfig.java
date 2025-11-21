package com.deployment.gitlab.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * Email configuration properties
 */
@Configuration
@ConfigurationProperties(prefix = "deployment.email")
@Data
public class EmailConfig {

    private String to;
    private String from;
    private Map<String, String> subjects = new HashMap<>();
}
