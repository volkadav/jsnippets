package com.norrisjackson.jsnippets.controllers.dto;

import com.norrisjackson.jsnippets.validation.ValidationConstants;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO for user registration requests with Bean Validation constraints.
 */
public record RegistrationRequest(
    @NotBlank(message = "Username is required")
    @Size(min = ValidationConstants.Username.MIN_LENGTH,
          max = ValidationConstants.Username.MAX_LENGTH,
          message = "Username must be between " + ValidationConstants.Username.MIN_LENGTH +
                    " and " + ValidationConstants.Username.MAX_LENGTH + " characters")
    String username,

    @NotBlank(message = "Password is required")
    @Size(min = ValidationConstants.Password.MIN_LENGTH,
          max = ValidationConstants.Password.MAX_LENGTH,
          message = "Password must be at least " + ValidationConstants.Password.MIN_LENGTH + " characters")
    String password,

    @NotBlank(message = "Password confirmation is required")
    String password2,

    @NotBlank(message = "Email is required")
    @Email(message = "Please enter a valid email address")
    String email
) {}

