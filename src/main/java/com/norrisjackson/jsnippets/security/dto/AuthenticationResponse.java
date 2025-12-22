package com.norrisjackson.jsnippets.security.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for authentication responses (login success)
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AuthenticationResponse {

    private String token;
    private String tokenType = "Bearer";
    private String username;
    private Long expiresIn; // in milliseconds

    public AuthenticationResponse(String token, String username, Long expiresIn) {
        this.token = token;
        this.username = username;
        this.expiresIn = expiresIn;
        this.tokenType = "Bearer";
    }
}

