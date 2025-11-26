package com.deployment.gitlab.controller;

import com.deployment.gitlab.model.Application;
import com.deployment.gitlab.model.CodeFreeze;
import com.deployment.gitlab.repository.ApplicationRepository;
import com.deployment.gitlab.service.CodeFreezeService;
import com.deployment.gitlab.service.GitLabService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Admin controller
 */
@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
@Slf4j
public class AdminController {

    private final CodeFreezeService codeFreezeService;
    private final GitLabService gitLabService;
    private final ApplicationRepository applicationRepository;

    @GetMapping("/login")
    public String login() {
        return "admin/login";
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model, Authentication authentication) {
        log.debug("Displaying admin dashboard");

        model.addAttribute("username", authentication.getName());
        model.addAttribute("codeFreeze", codeFreezeService.getCodeFreezeStatus());

        // Test GitLab connection
        boolean gitlabConnected = gitLabService.testGitLabConnection();
        model.addAttribute("gitlabConnected", gitlabConnected);

        // Count synchronized applications
        int syncedAppsCount = applicationRepository.findAllEnabled().size();
        model.addAttribute("syncedAppsCount", syncedAppsCount);

        return "admin/dashboard";
    }

    @GetMapping("/code-freeze")
    public String codeFreeze(Model model) {
        log.debug("Displaying code freeze page");

        CodeFreeze codeFreeze = codeFreezeService.getCodeFreezeStatus();
        List<Application> freezableApps = codeFreezeService.getAllFreezableApplications();

        // Sort applications: frozen ones first, then active ones
        freezableApps.sort((app1, app2) -> {
            boolean app1Frozen = codeFreeze.isApplicationFrozen(app1.getName());
            boolean app2Frozen = codeFreeze.isApplicationFrozen(app2.getName());

            if (app1Frozen == app2Frozen) {
                // If both have same freeze status, sort by name
                return app1.getName().compareToIgnoreCase(app2.getName());
            }
            // Frozen apps come first
            return app1Frozen ? -1 : 1;
        });

        model.addAttribute("codeFreeze", codeFreeze);
        model.addAttribute("applications", freezableApps);

        return "admin/code-freeze";
    }

    @PostMapping("/code-freeze/global/enable")
    public String enableGlobalFreeze(
            @RequestParam("reason") String reason,
            @RequestParam("endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) java.time.LocalDate endDate,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        log.info("Enabling global code freeze by {}", authentication.getName());

        try {
            // Convert LocalDate to LocalDateTime (end of day: 23:59:59)
            LocalDateTime endDateTime = endDate.atTime(23, 59, 59);
            codeFreezeService.enableGlobalFreeze(reason, endDateTime, authentication.getName());
            redirectAttributes.addFlashAttribute("success", "Global code freeze enabled");
        } catch (Exception e) {
            log.error("Error enabling global code freeze", e);
            redirectAttributes.addFlashAttribute("error", "Error: " + e.getMessage());
        }

        return "redirect:/admin/code-freeze";
    }

    @PostMapping("/code-freeze/global/disable")
    public String disableGlobalFreeze(RedirectAttributes redirectAttributes) {
        log.info("Disabling global code freeze");

        try {
            codeFreezeService.disableGlobalFreeze();
            redirectAttributes.addFlashAttribute("success", "Global code freeze disabled");
        } catch (Exception e) {
            log.error("Error disabling global code freeze", e);
            redirectAttributes.addFlashAttribute("error", "Error: " + e.getMessage());
        }

        return "redirect:/admin/code-freeze";
    }

    @PostMapping("/code-freeze/app/freeze")
    public String freezeApplication(
            @RequestParam("applicationName") String applicationName,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        log.info("Freezing application {} by {}", applicationName, authentication.getName());

        try {
            codeFreezeService.freezeApplication(applicationName, "Frozen by admin", authentication.getName());
            redirectAttributes.addFlashAttribute("success",
                    "Application '" + applicationName + "' gelée");
        } catch (Exception e) {
            log.error("Error freezing application {}", applicationName, e);
            redirectAttributes.addFlashAttribute("error", "Error: " + e.getMessage());
        }

        return "redirect:/admin/code-freeze";
    }

    @PostMapping("/code-freeze/app/unfreeze")
    public String unfreezeApplication(
            @RequestParam("applicationName") String applicationName,
            RedirectAttributes redirectAttributes) {

        log.info("Unfreezing application {}", applicationName);

        try {
            codeFreezeService.unfreezeApplication(applicationName);
            redirectAttributes.addFlashAttribute("success",
                    "Application '" + applicationName + "' dégelée");
        } catch (Exception e) {
            log.error("Error unfreezing application {}", applicationName, e);
            redirectAttributes.addFlashAttribute("error", "Error: " + e.getMessage());
        }

        return "redirect:/admin/code-freeze";
    }

    @PostMapping("/code-freeze/app/bulk-freeze")
    public String bulkFreezeApplications(
            @RequestParam("applicationNames") String applicationNames,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        String[] appNames = applicationNames.split(",");
        log.info("Bulk freezing {} applications by {}", appNames.length, authentication.getName());

        int successCount = 0;
        int failureCount = 0;

        for (String appName : appNames) {
            try {
                codeFreezeService.freezeApplication(appName.trim(), "Frozen by admin (bulk)", authentication.getName());
                successCount++;
            } catch (Exception e) {
                log.error("Error freezing application {} in bulk operation", appName, e);
                failureCount++;
            }
        }

        if (successCount > 0) {
            redirectAttributes.addFlashAttribute("success",
                    successCount + " application(s) gelée(s)");
        }
        if (failureCount > 0) {
            redirectAttributes.addFlashAttribute("error",
                    failureCount + " application(s) n'ont pas pu être gelée(s)");
        }

        return "redirect:/admin/code-freeze";
    }

    @PostMapping("/code-freeze/app/bulk-unfreeze")
    public String bulkUnfreezeApplications(
            @RequestParam("applicationNames") String applicationNames,
            RedirectAttributes redirectAttributes) {

        String[] appNames = applicationNames.split(",");
        log.info("Bulk unfreezing {} applications", appNames.length);

        int successCount = 0;
        int failureCount = 0;

        for (String appName : appNames) {
            try {
                codeFreezeService.unfreezeApplication(appName.trim());
                successCount++;
            } catch (Exception e) {
                log.error("Error unfreezing application {} in bulk operation", appName, e);
                failureCount++;
            }
        }

        if (successCount > 0) {
            redirectAttributes.addFlashAttribute("success",
                    successCount + " application(s) dégelée(s)");
        }
        if (failureCount > 0) {
            redirectAttributes.addFlashAttribute("error",
                    failureCount + " application(s) n'ont pas pu être dégelée(s)");
        }

        return "redirect:/admin/code-freeze";
    }
}
