package com.norrisjackson.jsnippets.configs;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for validation constraints.
 * Centralizes magic numbers for field length limits and other validation rules.
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
         * Default: 3 characters
         */
        private int minLength = 3;

        /**
         * Maximum username length.
         * Default: 50 characters
         */
        private int maxLength = 50;
    }

    @Getter
    @Setter
    public static class PasswordConstraints {
        /**
         * Minimum password length.
         * Default: 8 characters
         */
        private int minLength = 8;

        /**
         * Maximum password length.
         * Default: 100 characters
         */
        private int maxLength = 100;
    }

    @Getter
    @Setter
    public static class BioConstraints {
        /**
         * Maximum bio length.
         * Default: 4000 characters
         */
        private int maxLength = 4000;
    }

    @Getter
    @Setter
    public static class SnippetConstraints {
        /**
         * Maximum snippet content length.
         * Default: 10000 characters
         */
        private int maxLength = 10000;
    }
}

