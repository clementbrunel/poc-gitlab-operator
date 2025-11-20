package com.deployment.gitlab.repository;

import com.deployment.gitlab.model.Application;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Repository;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Repository to manage applications (in memory, loaded from applications.yaml)
 */
@Repository
@Slf4j
public class ApplicationRepository {

    private final Map<String, Application> applications = new HashMap<>();
    private final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());

    /**
     * Loads applications from the applications.yaml file at startup
     */
    @PostConstruct
    public void loadApplications() {
        try {
            ClassPathResource resource = new ClassPathResource("applications.yaml");
            if (!resource.exists()) {
                log.warn("applications.yaml file not found, no applications loaded");
                return;
            }

            try (InputStream inputStream = resource.getInputStream()) {
                ApplicationsConfig config = yamlMapper.readValue(inputStream, ApplicationsConfig.class);
                if (config != null && config.applications != null) {
                    for (Application app : config.applications) {
                        applications.put(app.getName(), app);
                    }
                    log.info("Loaded {} application(s) from applications.yaml", applications.size());
                }
            }
        } catch (IOException e) {
            log.error("Error loading applications.yaml file", e);
        }
    }

    /**
     * Retrieves all applications
     */
    public List<Application> findAll() {
        return new ArrayList<>(applications.values());
    }

    /**
     * Retrieves all enabled applications
     */
    public List<Application> findAllEnabled() {
        return applications.values().stream()
                .filter(Application::isEnabled)
                .collect(Collectors.toList());
    }

    /**
     * Retrieves an application by its name
     */
    public Optional<Application> findByName(String name) {
        return Optional.ofNullable(applications.get(name));
    }

    /**
     * Retrieves an application by its GitLab ID
     */
    public Optional<Application> findByGitlabProjectId(Integer projectId) {
        return applications.values().stream()
                .filter(app -> app.getGitlabProjectId().equals(projectId))
                .findFirst();
    }

    /**
     * Retrieves all applications that can be frozen
     */
    public List<Application> findAllFreezable() {
        return applications.values().stream()
                .filter(Application::isFreezable)
                .collect(Collectors.toList());
    }

    /**
     * Internal class for YAML parsing
     */
    private static class ApplicationsConfig {
        public List<Application> applications;
    }
}
