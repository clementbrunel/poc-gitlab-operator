package com.deployment.gitlab.controller;

import com.deployment.gitlab.model.ApplicationArtifactInfo;
import com.deployment.gitlab.service.GitLabService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST API Controller for deployment-related operations
 */
@RestController
@RequestMapping("/rest/deployment")
@RequiredArgsConstructor
@Slf4j
public class DeploymentApiController {

    private final GitLabService gitLabService;

    /**
     * Retrieves artifact information for multiple applications
     *
     * @param applicationNames List of application names
     * @return List of artifact information for each application
     */
    @PostMapping("/artifacts")
    public ResponseEntity<List<ApplicationArtifactInfo>> getArtifacts(
            @RequestBody List<String> applicationNames) {

        log.info("API call to retrieve artifacts for {} application(s)", applicationNames.size());

        if (applicationNames.isEmpty()) {
            log.warn("Empty application names list received");
            return ResponseEntity.badRequest().build();
        }

        try {
            List<ApplicationArtifactInfo> artifacts = gitLabService.getMultipleApplicationArtifacts(applicationNames);
            return ResponseEntity.ok(artifacts);
        } catch (Exception e) {
            log.error("Error retrieving artifacts: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
