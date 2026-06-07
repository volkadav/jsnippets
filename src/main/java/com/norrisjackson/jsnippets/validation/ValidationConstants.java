package com.norrisjackson.jsnippets.validation;

/**
 * Validation constants for Bean Validation annotations.
 * These constants are used in @Size, @Min, @Max annotations which require compile-time constants.
 *
 * These values are the canonical defaults. ValidationConfig (backed by
 * application.properties) starts from the same defaults and may be overridden at
 * runtime for manual validation checks in controllers/services, but Bean Validation
 * annotations always use these compile-time constants and cannot be changed at runtime.
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

