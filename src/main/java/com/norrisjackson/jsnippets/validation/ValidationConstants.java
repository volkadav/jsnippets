package com.norrisjackson.jsnippets.validation;

/**
 * Validation constants for Bean Validation annotations.
 * These constants are used in @Size, @Min, @Max annotations which require compile-time constants.
 *
 * The actual runtime values can be overridden via ValidationConfig and application.properties,
 * but these constants serve as:
 * 1. Documentation of default validation rules
 * 2. Compile-time constants for Bean Validation annotations
 * 3. Centralized location for all validation magic numbers
 */
public final class ValidationConstants {

    private ValidationConstants() {
        // Utility class, prevent instantiation
    }

    /**
     * Username validation constraints.
     */
    public static final class Username {
        public static final int MIN_LENGTH = 3;
        public static final int MAX_LENGTH = 50;

        private Username() {}
    }

    /**
     * Password validation constraints.
     */
    public static final class Password {
        public static final int MIN_LENGTH = 8;
        public static final int MAX_LENGTH = 100;

        private Password() {}
    }

    /**
     * Bio validation constraints.
     */
    public static final class Bio {
        public static final int MAX_LENGTH = 4000;

        private Bio() {}
    }

    /**
     * Snippet validation constraints.
     */
    public static final class Snippet {
        public static final int MAX_LENGTH = 10000;

        private Snippet() {}
    }
}

