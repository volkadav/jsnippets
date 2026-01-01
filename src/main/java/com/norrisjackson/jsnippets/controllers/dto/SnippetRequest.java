package com.norrisjackson.jsnippets.controllers.dto;

import com.norrisjackson.jsnippets.validation.ValidationConstants;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO for snippet creation/update requests with Bean Validation constraints.
 */
public record SnippetRequest(
    @NotBlank(message = "Snippet content is required")
    @Size(max = ValidationConstants.Snippet.MAX_LENGTH,
          message = "Snippet content must be " +
            ValidationConstants.Snippet.MAX_LENGTH + " characters or less")
    String contents
) {}

