package com.deployment.gitlab.controller;

import com.deployment.gitlab.model.Application;
import com.deployment.gitlab.model.DeploymentRequest;
import com.deployment.gitlab.repository.ApplicationRepository;
import com.deployment.gitlab.service.CodeFreezeService;
import com.deployment.gitlab.service.DeploymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * Controller for the deployment page
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class DeploymentController {

    private final ApplicationRepository applicationRepository;
    private final DeploymentService deploymentService;
    private final CodeFreezeService codeFreezeService;

    @GetMapping("/deployment")
    public String deployment(Model model) {
        log.debug("Displaying deployment page");

        List<Application> applications = applicationRepository.findAllEnabled();
        model.addAttribute("applications", applications);
        model.addAttribute("codeFreeze", codeFreezeService.getCodeFreezeStatus());

        return "deployment";
    }

    @PostMapping("/deployment")
    public String submitDeployment(
            @RequestParam("applicationNames") List<String> applicationNames,
            @RequestParam("requesterName") String requesterName,
            @RequestParam("requesterEmail") String requesterEmail,
            @RequestParam("targetEnvironment") String targetEnvironment,
            @RequestParam(value = "notes", required = false) String notes,
            RedirectAttributes redirectAttributes) {

        log.info("Submitting deployment request by {}", requesterName);

        try {
            DeploymentRequest request = DeploymentRequest.builder()
                    .applicationNames(applicationNames)
                    .requesterName(requesterName)
                    .requesterEmail(requesterEmail)
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
