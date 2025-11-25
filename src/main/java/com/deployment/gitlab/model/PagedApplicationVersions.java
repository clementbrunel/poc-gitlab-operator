package com.deployment.gitlab.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Paginated response for application versions
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PagedApplicationVersions {

    private List<ApplicationVersion> content;
    private int currentPage;
    private int pageSize;
    private long totalElements;
    private int totalPages;
    private boolean first;
    private boolean last;

    public static PagedApplicationVersions of(List<ApplicationVersion> allVersions, int page, int pageSize) {
        int totalElements = allVersions.size();
        int totalPages = (int) Math.ceil((double) totalElements / pageSize);

        // Ensure page is within bounds
        page = Math.max(0, Math.min(page, totalPages - 1));

        int start = page * pageSize;
        int end = Math.min(start + pageSize, totalElements);

        List<ApplicationVersion> content = start < totalElements
            ? allVersions.subList(start, end)
            : List.of();

        return PagedApplicationVersions.builder()
                .content(content)
                .currentPage(page)
                .pageSize(pageSize)
                .totalElements(totalElements)
                .totalPages(totalPages)
                .first(page == 0)
                .last(page >= totalPages - 1)
                .build();
    }
}
