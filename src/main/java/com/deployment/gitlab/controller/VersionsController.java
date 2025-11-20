package com.deployment.gitlab.controller;

import com.deployment.gitlab.model.ApplicationVersion;
import com.deployment.gitlab.service.GitLabService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

/**
 * Controller for the versions page
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class VersionsController {

    private final GitLabService gitLabService;

    @GetMapping("/versions")
    public String versions(Model model) {
        log.debug("Displaying versions page");

        try {
            List<ApplicationVersion> versions = gitLabService.getAllApplicationVersions();
            model.addAttribute("versions", versions);
            model.addAttribute("error", null);
        } catch (Exception e) {
            log.error("Error retrieving versions", e);
            model.addAttribute("versions", List.of());
            model.addAttribute("error", "Error retrieving versions: " + e.getMessage());
        }

        return "versions";
    }
}
