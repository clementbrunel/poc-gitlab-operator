package com.deployment.gitlab.controller;

import com.deployment.gitlab.model.Application;
import com.deployment.gitlab.model.CodeFreeze;
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

        return "admin/dashboard";
    }

    @GetMapping("/code-freeze")
    public String codeFreeze(Model model) {
        log.debug("Displaying code freeze page");

        CodeFreeze codeFreeze = codeFreezeService.getCodeFreezeStatus();
        List<Application> freezableApps = codeFreezeService.getAllFreezableApplications();

        model.addAttribute("codeFreeze", codeFreeze);
        model.addAttribute("applications", freezableApps);

        return "admin/code-freeze";
    }

    @PostMapping("/code-freeze/global/enable")
    public String enableGlobalFreeze(
            @RequestParam("reason") String reason,
            @RequestParam("endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        log.info("Enabling global code freeze by {}", authentication.getName());

        try {
            codeFreezeService.enableGlobalFreeze(reason, endDate, authentication.getName());
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
                    "Application '" + applicationName + "' frozen");
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
                    "Application '" + applicationName + "' unfrozen");
        } catch (Exception e) {
            log.error("Error unfreezing application {}", applicationName, e);
            redirectAttributes.addFlashAttribute("error", "Error: " + e.getMessage());
        }

        return "redirect:/admin/code-freeze";
    }
}
