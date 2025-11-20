package com.deployment.gitlab;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main application - GitLab Deployment Manager
 *
 * This application manages deployments in cooperation with GitLab.
 * It provides:
 * - Visualization of versions on the develop branch
 * - Management of deployment requests via email
 * - Code freeze management by an administrator
 */
@SpringBootApplication
@EnableScheduling
public class DeploymentManagerApplication {

    public static void main(String[] args) {
        SpringApplication.run(DeploymentManagerApplication.class, args);
    }
}
