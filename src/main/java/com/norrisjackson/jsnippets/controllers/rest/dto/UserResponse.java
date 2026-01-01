package com.norrisjackson.jsnippets.controllers.rest.dto;

import com.norrisjackson.jsnippets.data.User;

/**
 * DTO for User data in API responses.
 * Exposes only safe, public fields - excludes password, email, and other sensitive data.
 */
public record UserResponse(
    Long id,
    String username,
    String bio
) {
    /**
     * Create a UserResponse from a User entity.
     * Only includes publicly visible fields.
     */
    public static UserResponse from(User user) {
        return new UserResponse(
            user.getId(),
            user.getUsername(),
            user.getBio()
        );
    }
}

