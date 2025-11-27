package com.deployment.gitlab.service;

import com.deployment.gitlab.config.GitLabClient;
import com.deployment.gitlab.config.GitLabConfig;
import com.deployment.gitlab.model.*;
import com.deployment.gitlab.repository.ApplicationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;

/**
 * Service to interact with GitLab
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GitLabService {

    private final GitLabClient gitLabClient;
    private final GitLabConfig gitLabConfig;
    private final ApplicationRepository applicationRepository;
    private final CodeFreezeService codeFreezeService;
    private final Random random = new Random();

    /**
     * Retrieves versions of all active applications
     */
    public List<ApplicationVersion> getAllApplicationVersions() {
        List<Application> applications = applicationRepository.findAllEnabled();
        List<ApplicationVersion> versions = new ArrayList<>();

        for (Application app : applications) {
            ApplicationVersion version = getApplicationVersion(app);
            if (version != null) {
                versions.add(version);
            }
        }

        return versions;
    }

    /**
     * Retrieves versions of all active applications with pagination
     * Only fetches commit information for applications on the requested page
     *
     * @param page Page number (0-indexed)
     * @param pageSize Number of items per page
     * @param searchQuery Optional search query to filter applications by name
     * @return Paginated application versions
     */
    public PagedApplicationVersions getAllApplicationVersionsPaged(int page, int pageSize, String searchQuery) {
        List<Application> allApplications = applicationRepository.findAllEnabled();

        // Filter applications by search query if provided
        if (searchQuery != null && !searchQuery.trim().isEmpty()) {
            String lowerQuery = searchQuery.trim().toLowerCase();
            allApplications = allApplications.stream()
                .filter(app -> app.getName().toLowerCase().contains(lowerQuery) ||
                              (app.getDescription() != null && app.getDescription().toLowerCase().contains(lowerQuery)))
                .toList();
        }

        // Calculate pagination boundaries
        int totalElements = allApplications.size();
        int totalPages = (int) Math.ceil((double) totalElements / pageSize);

        // Ensure page is within bounds
        page = Math.max(0, Math.min(page, Math.max(0, totalPages - 1)));

        int start = page * pageSize;
        int end = Math.min(start + pageSize, totalElements);

        // Only fetch commit info for applications on the current page
        List<Application> pageApplications = start < totalElements
            ? allApplications.subList(start, end)
            : List.of();

        List<ApplicationVersion> versions = new ArrayList<>();
        for (Application app : pageApplications) {
            ApplicationVersion version = getApplicationVersion(app);
            if (version != null) {
                versions.add(version);
            }
        }

        return PagedApplicationVersions
                .builder()
                .content(versions)
                .currentPage(page)
                .pageSize(pageSize)
                .totalElements(totalElements)
                .totalPages(totalPages)
                .first(page == 0)
                .last(page >= totalPages - 1)
                .build();
    }

    /**
     * Retrieves the version of a specific application
     */
    public ApplicationVersion getApplicationVersion(Application app) {
        try {
            log.debug("Retrieving version for application: {}", app.getName());

            // DEMO MODE: If GitLab is not configured, return demo data
            if (!gitLabConfig.isConfigured()) {
                return createDemoVersion(app);
            }

            // Retrieves the last commit of the branch
            GitLabCommit commit = gitLabClient.getLastCommit(
                    app.getGitlabProjectId(),
                    app.getBranch()
            );

            if (commit == null) {
                log.warn("Unable to retrieve commit for application: {}", app.getName());
                return createDemoVersion(app); // Fallback to demo data
            }

            // Builds the version
            return ApplicationVersion
                    .builder()
                    .application(app)
                    .version(extractVersion(commit))
                    .commitSha(commit.getShortId())
                    .commitMessage(commit.getTitle())
                    .commitAuthor(commit.getAuthorName())
                    .commitDate(commit.getCommittedDate() != null
                        ? commit.getCommittedDate().toLocalDateTime()
                        : null)
                    .commitUrl(commit.getWebUrl())
                    .frozen(codeFreezeService.isApplicationFrozen(app.getName()))
                    .build();
        } catch (Exception e) {
            log.error("Error retrieving version for {}: {}",
                    app.getName(), e.getMessage());
            return createDemoVersion(app); // Fallback to demo data
        }
    }

    /**
     * Creates demo version data for testing without GitLab connection
     */
    private ApplicationVersion createDemoVersion(Application app) {
        String[] demoCommitMessages = {
                "feat: Add new feature for user management",
                "fix: Correct bug in authentication flow",
                "chore: Update dependencies to latest versions",
                "refactor: Improve code structure",
                "perf: Optimize database queries",
                "docs: Update API documentation",
                "test: Add unit tests for core functionality"
        };

        String[] demoAuthors = {
                "Alice Developer", "Bob Engineer", "Charlie DevOps",
                "Diana Architect", "Eve Tester", "Frank Admin"
        };

        // Generate realistic demo data
        String commitSha = String.format("%08x", random.nextInt());
        String commitMessage = demoCommitMessages[random.nextInt(demoCommitMessages.length)];
        String author = demoAuthors[random.nextInt(demoAuthors.length)];
        LocalDateTime commitDate = LocalDateTime.now().minusDays(random.nextInt(30));

        return ApplicationVersion
                .builder()
                .application(app)
                .version("v1." + random.nextInt(10) + "." + random.nextInt(100))
                .commitSha(commitSha)
                .commitMessage(commitMessage)
                .commitAuthor(author)
                .commitDate(commitDate)
                .commitUrl("#demo")
                .frozen(codeFreezeService.isApplicationFrozen(app.getName()))
                .build();
    }

    /**
     * Extracts a version from a commit
     * Searches for tags or uses the short SHA
     */
    private String extractVersion(GitLabCommit commit) {
        // For now, we use the short SHA
        // TODO: Improve to extract tags if available
        return commit.getShortId();
    }

    /**
     * Tests the GitLab connection
     */
    public boolean testGitLabConnection() {
        return gitLabClient.testConnection();
    }

    /**
     * Retrieves artifact information (.ear files) for a single application
     */
    public ApplicationArtifactInfo getApplicationArtifacts(Application app) {
        try {
            log.debug("Retrieving artifacts for application: {}", app.getName());

            // Step 1: Get the latest successful pipeline for the branch
            List<GitLabPipeline> pipelines = gitLabClient.getPipelines(
                    app.getGitlabProjectId(),
                    app.getBranch()
            );

            if (pipelines == null || pipelines.isEmpty()) {
                log.warn("No pipeline found for {} on branch {}", app.getName(), app.getBranch());
                return ApplicationArtifactInfo
                        .builder()
                        .applicationName(app.getName())
                        .success(false)
                        .errorMessage("Aucune pipeline trouvée sur la branche " + app.getBranch())
                        .artifacts(List.of())
                        .build();
            }

            // Get the most recent successful pipeline
            List<String> authorizedStatus = List.of("success", "manual");
            GitLabPipeline latestSuccessPipeline = pipelines.stream()
                         .filter(pipe -> authorizedStatus.contains(pipe.getStatus()))
                         .findFirst().orElse(null);

            if (latestSuccessPipeline == null) {
                log.warn("No successful pipeline found for {} on branch {}", app.getName(), app.getBranch());
                return ApplicationArtifactInfo
                        .builder()
                        .applicationName(app.getName())
                        .success(false)
                        .errorMessage("Aucune pipeline réussie trouvée sur la branche " + app.getBranch())
                        .artifacts(List.of())
                        .build();
            }

            // Step 2: Get all jobs for this pipeline
            List<GitLabJob> jobs = gitLabClient.getJobs(app.getGitlabProjectId(), latestSuccessPipeline.getId());

            if (jobs == null || jobs.isEmpty()) {
                log.warn("No jobs found for pipeline {} of application {}", latestSuccessPipeline.getId(), app.getName());
                return ApplicationArtifactInfo
                        .builder()
                        .applicationName(app.getName())
                        .success(false)
                        .errorMessage("Aucun job trouvé dans la pipeline")
                        .artifacts(List.of())
                        .build();
            }

            // Step 3: Find the configured job (e.g., "jobftp")
            String jobName = gitLabConfig.getArtifactJobName();
            GitLabJob targetJob = jobs.stream()
                    .filter(job -> jobName.equalsIgnoreCase(job.getName()))
                    .findFirst()
                    .orElse(null);

            if (targetJob == null) {
                log.warn("No successful '{}' job found for application {}", jobName, app.getName());
                return ApplicationArtifactInfo
                        .builder()
                        .applicationName(app.getName())
                        .success(false)
                        .errorMessage("Aucun job '" + jobName + "' réussi trouvé")
                        .artifacts(List.of())
                        .build();
            } else if (!"success".equalsIgnoreCase(targetJob.getStatus())) {
                return ApplicationArtifactInfo
                        .builder()
                        .applicationName(app.getName())
                        .success(false)
                        .errorMessage("Le job '" + jobName + "' de la dernière pipeline réussie est au statut '" + targetJob.getStatus() + "'")
                        .artifacts(List.of())
                        .build();
            }

            // Step 4: Get trace (logs) from the job
            String trace = gitLabClient.getJobTrace(app.getGitlabProjectId(), targetJob.getId());

            if (trace.isEmpty()) {
                log.warn("No trace found for job {} of application {}", targetJob.getId(), app.getName());
                return ApplicationArtifactInfo
                        .builder()
                        .applicationName(app.getName())
                        .success(false)
                        .errorMessage("Aucun log trouvé pour le job '" + jobName + "'")
                        .artifacts(List.of())
                        .buildDate(targetJob.getFinishedAt() != null
                                ? targetJob.getFinishedAt().toLocalDateTime()
                                : null)
                        .jobUrl(targetJob.getWebUrl())
                        .build();
            }

            // Step 5: Extract .ear files from trace
            List<String> earFiles = gitLabClient.extractEarFilesFromTrace(trace);

            if (earFiles.isEmpty()) {
                log.warn("No .ear files found in trace for job {} of application {}", targetJob.getId(), app.getName());
                return ApplicationArtifactInfo
                        .builder()
                        .applicationName(app.getName())
                        .success(false)
                        .errorMessage("Aucun fichier .ear trouvé dans les logs du job '" + jobName + "'")
                        .artifacts(List.of())
                        .buildDate(targetJob.getFinishedAt() != null
                                ? targetJob.getFinishedAt().toLocalDateTime()
                                : null)
                        .jobUrl(targetJob.getWebUrl())
                        .build();
            }

            // Success!
            return ApplicationArtifactInfo
                    .builder()
                    .applicationName(app.getName())
                    .success(true)
                    .artifacts(earFiles)
                    .buildDate(targetJob.getFinishedAt() != null
                            ? targetJob.getFinishedAt().toLocalDateTime()
                            : null)
                    .jobUrl(targetJob.getWebUrl())
                    .build();

        } catch (Exception e) {
            log.error("Error retrieving artifacts for application {}: {}", app.getName(), e.getMessage(), e);
            return ApplicationArtifactInfo
                    .builder()
                    .applicationName(app.getName())
                    .success(false)
                    .errorMessage("Erreur technique: " + e.getMessage())
                    .artifacts(List.of())
                    .build();
        }
    }

    /**
     * Retrieves artifact information for multiple applications
     * Processes in parallel for better performance
     */
    public List<ApplicationArtifactInfo> getMultipleApplicationArtifacts(List<String> applicationNames) {
        log.info("Retrieving artifacts for {} application(s)", applicationNames.size());

        List<ApplicationArtifactInfo> results = new ArrayList<>();

        for (String appName : applicationNames) {
            // Find the application
            Optional<Application> app = applicationRepository.findByName(appName);

            if (app.isEmpty()) {
                log.warn("Application not found: {}", appName);
                results.add(ApplicationArtifactInfo
                        .builder()
                        .applicationName(appName)
                        .success(false)
                        .errorMessage("Application non trouvée")
                        .artifacts(List.of())
                        .build());
                continue;
            }

            // Get artifacts for this application
            ApplicationArtifactInfo artifactInfo = getApplicationArtifacts(app.get());
            results.add(artifactInfo);
        }

        return results;
    }
}
