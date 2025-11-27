package com.deployment.gitlab.controller;

import com.deployment.gitlab.model.Application;
import com.deployment.gitlab.model.DeploymentRequest;
import com.deployment.gitlab.repository.ApplicationRepository;
import com.deployment.gitlab.service.CodeFreezeService;
import com.deployment.gitlab.service.DeploymentService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller for the deployment page
 */
@Controller
@RequestMapping("/deployment")
@RequiredArgsConstructor
@Slf4j
public class DeploymentController {

    private final ApplicationRepository applicationRepository;
    private final DeploymentService deploymentService;
    private final CodeFreezeService codeFreezeService;
    private final ObjectMapper objectMapper;

    @GetMapping()
    public String deployment(Model model) {
        log.debug("Displaying deployment page");

        List<Application> applications = applicationRepository.findAllEnabled();
        // Sort applications alphabetically by name
        applications.sort((app1, app2) -> app1.getName().compareToIgnoreCase(app2.getName()));

        model.addAttribute("applications", applications);
        model.addAttribute("codeFreeze", codeFreezeService.getCodeFreezeStatus());

        return "deployment";
    }

    @PostMapping()
    public String submitDeployment(
            @RequestParam("applicationNames") List<String> applicationNames,
            @RequestParam("requesterName") String requesterName,
            @RequestParam("targetEnvironment") String targetEnvironment,
            @RequestParam(value = "notes", required = false) String notes,
            @RequestParam(value = "artifactsJson", required = false) String artifactsJson,
            RedirectAttributes redirectAttributes) {

        log.info("Submitting deployment request by {}", requesterName);

        try {
            // Parse artifacts JSON
            Map<String, List<String>> artifacts = new HashMap<>();
            if (artifactsJson != null && !artifactsJson.isEmpty()) {
                try {
                    artifacts = objectMapper.readValue(artifactsJson,
                            new TypeReference<Map<String, List<String>>>() {});
                    log.debug("Parsed {} artifacts from JSON", artifacts.size());
                } catch (Exception e) {
                    log.warn("Failed to parse artifacts JSON: {}", e.getMessage());
                }
            }

            DeploymentRequest request = DeploymentRequest.builder()
                    .applicationNames(applicationNames)
                    .artifacts(artifacts)
                    .requesterName(requesterName)
                    .targetEnvironment(targetEnvironment)
                    .notes(notes)
                    .build();

            deploymentService.submitDeploymentRequest(request);

            redirectAttributes.addFlashAttribute("success",
                    "Deployment request sent successfully!");

        } catch (IllegalStateException e) {
            log.warn("Deployment request rejected: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        } catch (Exception e) {
            log.error("Error submitting deployment request", e);
            redirectAttributes.addFlashAttribute("error",
                    "Error sending request: " + e.getMessage());
        }

        return "redirect:/deployment";
    }
}
