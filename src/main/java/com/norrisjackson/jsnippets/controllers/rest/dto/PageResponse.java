package com.norrisjackson.jsnippets.controllers.rest.dto;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * DTO for paginated API responses.
 * Provides a stable pagination contract with only the necessary fields.
 *
 * @param <T> the type of content in the page
 */
public record PageResponse<T>(
    List<T> content,
    int pageNumber,
    int pageSize,
    long totalElements,
    int totalPages,
    boolean first,
    boolean last,
    boolean empty
) {
    /**
     * Create a PageResponse from a Spring Data Page, mapping content with a converter function.
     *
     * @param page the Spring Data page
     * @param content the already-converted content list
     * @param <T> the target type
     * @return a PageResponse with the converted content
     */
    public static <T> PageResponse<T> from(Page<?> page, List<T> content) {
        return new PageResponse<>(
            content,
            page.getNumber(),
            page.getSize(),
            page.getTotalElements(),
            page.getTotalPages(),
            page.isFirst(),
            page.isLast(),
            page.isEmpty()
        );
    }
}

