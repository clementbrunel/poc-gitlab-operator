package com.deployment.gitlab.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.StringHttpMessageConverter;

import java.nio.charset.StandardCharsets;

/**
 * Configuration to ensure UTF-8 encoding across the application
 */
@Configuration
public class EncodingConfig {

    /**
     * Configure HTTP message converter to use UTF-8
     */
    @Bean
    public StringHttpMessageConverter stringHttpMessageConverter() {
        return new StringHttpMessageConverter(StandardCharsets.UTF_8);
    }
}
