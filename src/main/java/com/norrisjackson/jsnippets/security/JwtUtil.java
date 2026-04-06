package com.norrisjackson.jsnippets.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.core.env.Environment;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date; // Required by jjwt library API
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Utility class for JWT token generation, validation, and extraction.
 * This is a singleton bean that is thread-safe as it maintains no mutable state
 * beyond the injected configuration values which are set once at startup.
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
@Slf4j
public class JwtUtil {

    private static final String DEFAULT_SECRET = "jsnippets-super-secret-key-change-this-in-production-minimum-256-bits";

    @Value("${jwt.secret:" + DEFAULT_SECRET + "}")
    private String secret;

    @Value("${jwt.expiration:86400000}") // 24 hours in milliseconds
    private Long expiration;

    @Autowired(required = false)
    private Environment environment;

    /**
     * Validate that the JWT secret has been changed from the default in production.
     * Throws IllegalStateException if the default secret is used with a production profile.
     */
    @PostConstruct
    public void validateConfiguration() {
        if (DEFAULT_SECRET.equals(secret)) {
            if (isProductionProfile()) {
                throw new IllegalStateException(
                    "JWT secret must be changed from the default value in production! " +
                    "Set the JWT_SECRET environment variable to a secure, random value of at least 256 bits (32 characters)."
                );
            }
            log.warn("Using default JWT secret — acceptable for development but MUST be changed for production!");
        }
    }

    private boolean isProductionProfile() {
        if (environment == null) {
            return false;
        }
        for (String profile : environment.getActiveProfiles()) {
            String p = profile.toLowerCase();
            if ("prod".equals(p) || "production".equals(p)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Extract username from JWT token
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Extract expiration date from JWT token
     */
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * Extract a specific claim from JWT token
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Extract all claims from JWT token
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Check if token is expired
     */
    private Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    /**
     * Generate JWT token for user
     */
    public String generateToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        return createToken(claims, userDetails.getUsername());
    }

    /**
     * Create JWT token with claims and subject
     */
    private String createToken(Map<String, Object> claims, String subject) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey(), Jwts.SIG.HS256)
                .compact();
    }

    /**
     * Get signing key for JWT
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Validate JWT token
     */
    public Boolean validateToken(String token, UserDetails userDetails) {
        try {
            final String username = extractUsername(token);
            return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
        } catch (Exception e) {
            log.error("JWT validation failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Validate JWT token without UserDetails (basic validation)
     */
    public Boolean validateToken(String token) {
        try {
            extractAllClaims(token);
            return !isTokenExpired(token);
        } catch (Exception e) {
            log.error("JWT validation failed: {}", e.getMessage());
            return false;
        }
    }
}

