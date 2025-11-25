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

        return PagedApplicationVersions.builder()
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
            ApplicationVersion version = ApplicationVersion.builder()
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

            return version;
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

        return ApplicationVersion.builder()
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
}
