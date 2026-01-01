package com.norrisjackson.jsnippets.controllers.rest.dto;

import com.norrisjackson.jsnippets.data.Snippet;

import java.time.Instant;

/**
 * DTO for Snippet data in API responses.
 * Provides a stable API contract decoupled from the entity.
 */
public record SnippetResponse(
    Long id,
    String contents,
    UserResponse poster,
    Instant createdAt,
    Instant editedAt
) {
    /**
     * Create a SnippetResponse from a Snippet entity.
     */
    public static SnippetResponse from(Snippet snippet) {
        return new SnippetResponse(
            snippet.getId(),
            snippet.getContents(),
            UserResponse.from(snippet.getPoster()),
            snippet.getCreatedAt(),
            snippet.getEditedAt()
        );
    }
}

