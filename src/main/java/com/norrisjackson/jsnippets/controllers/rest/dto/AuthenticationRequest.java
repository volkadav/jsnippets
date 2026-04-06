package com.norrisjackson.jsnippets.controllers.rest.dto;

import com.norrisjackson.jsnippets.validation.ValidationConstants;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for authentication requests (login)
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AuthenticationRequest {

    @NotBlank(message = "Username is required")
    @Size(min = ValidationConstants.Username.MIN_LENGTH,
          max = ValidationConstants.Username.MAX_LENGTH,
          message = "Username must be between " + ValidationConstants.Username.MIN_LENGTH +
                    " and " + ValidationConstants.Username.MAX_LENGTH + " characters")
    private String username;

    @NotBlank(message = "Password is required")
    @Size(min = ValidationConstants.Password.MIN_LENGTH,
          max = ValidationConstants.Password.MAX_LENGTH,
          message = "Password must be between " + ValidationConstants.Password.MIN_LENGTH +
                    " and " + ValidationConstants.Password.MAX_LENGTH + " characters")
    private String password;
}

