package com.norrisjackson.jsnippets.configs;

import com.norrisjackson.jsnippets.validation.ValidationConstants;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for validation constraints.
 * Centralizes magic numbers for field length limits and other validation rules.
 *
 * Defaults are sourced from {@link ValidationConstants} to avoid duplication.
 * These properties can be overridden in application.properties for manual
 * validation checks in controllers/services. Note that Bean Validation
 * annotations on DTOs use compile-time constants and cannot be changed at runtime.
 */
@Configuration
@ConfigurationProperties(prefix = "validation")
@Getter
@Setter
public class ValidationConfig {

    /**
     * Username validation constraints.
     */
    private UsernameConstraints username = new UsernameConstraints();

    /**
     * Password validation constraints.
     */
    private PasswordConstraints password = new PasswordConstraints();

    /**
     * Bio validation constraints.
     */
    private BioConstraints bio = new BioConstraints();

    /**
     * Snippet validation constraints.
     */
    private SnippetConstraints snippet = new SnippetConstraints();

    @Getter
    @Setter
    public static class UsernameConstraints {
        /**
         * Minimum username length.
         * Default: sourced from ValidationConstants.Username.MIN_LENGTH
         */
        private int minLength = ValidationConstants.Username.MIN_LENGTH;

        /**
         * Maximum username length.
         * Default: sourced from ValidationConstants.Username.MAX_LENGTH
         */
        private int maxLength = ValidationConstants.Username.MAX_LENGTH;
    }

    @Getter
    @Setter
    public static class PasswordConstraints {
        /**
         * Minimum password length.
         * Default: sourced from ValidationConstants.Password.MIN_LENGTH
         */
        private int minLength = ValidationConstants.Password.MIN_LENGTH;

        /**
         * Maximum password length.
         * Default: sourced from ValidationConstants.Password.MAX_LENGTH
         */
        private int maxLength = ValidationConstants.Password.MAX_LENGTH;
    }

    @Getter
    @Setter
    public static class BioConstraints {
        /**
         * Maximum bio length.
         * Default: sourced from ValidationConstants.Bio.MAX_LENGTH
         */
        private int maxLength = ValidationConstants.Bio.MAX_LENGTH;
    }

    @Getter
    @Setter
    public static class SnippetConstraints {
        /**
         * Maximum snippet content length.
         * Default: sourced from ValidationConstants.Snippet.MAX_LENGTH
         */
        private int maxLength = ValidationConstants.Snippet.MAX_LENGTH;
    }
}

