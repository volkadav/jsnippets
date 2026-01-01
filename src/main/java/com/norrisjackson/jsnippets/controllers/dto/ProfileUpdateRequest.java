package com.norrisjackson.jsnippets.controllers.dto;

import com.norrisjackson.jsnippets.validation.ValidationConstants;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO for profile update requests with Bean Validation constraints.
 */
public record ProfileUpdateRequest(
    @NotBlank(message = "Email is required")
    @Email(message = "Please enter a valid email address")
    String email,

    @NotBlank(message = "Timezone is required")
    String timezone,

    @Size(max = ValidationConstants.Bio.MAX_LENGTH,
          message = "Bio must be " + ValidationConstants.Bio.MAX_LENGTH + " characters or less")
    String bio
) {}

