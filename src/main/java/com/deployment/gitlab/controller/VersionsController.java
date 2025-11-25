package com.deployment.gitlab.controller;

import com.deployment.gitlab.model.PagedApplicationVersions;
import com.deployment.gitlab.service.GitLabService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Controller for the versions page
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class VersionsController {

    private final GitLabService gitLabService;
    private static final int DEFAULT_PAGE_SIZE = 10;

    @GetMapping("/versions")
    public String versions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search,
            Model model) {
        log.debug("Displaying versions page (page: {}, size: {}, search: {})", page, size, search);

        try {
            // Ensure page size is reasonable (between 5 and 100)
            int pageSize = Math.max(5, Math.min(size, 100));

            PagedApplicationVersions pagedVersions = gitLabService.getAllApplicationVersionsPaged(page, pageSize, search);

            model.addAttribute("versions", pagedVersions.getContent());
            model.addAttribute("page", pagedVersions);
            model.addAttribute("search", search);
            model.addAttribute("error", null);
        } catch (Exception e) {
            log.error("Error retrieving versions", e);
            model.addAttribute("versions", java.util.List.of());
            model.addAttribute("page", null);
            model.addAttribute("search", search);
            model.addAttribute("error", "Error retrieving versions: " + e.getMessage());
        }

        return "versions";
    }
}
