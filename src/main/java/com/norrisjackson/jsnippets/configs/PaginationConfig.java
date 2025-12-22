package com.norrisjackson.jsnippets.configs;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for API pagination settings.
 * Values are loaded from application.properties with prefix "api.pagination".
 */
@Configuration
@ConfigurationProperties(prefix = "api.pagination")
@Getter
@Setter
public class PaginationConfig {

    /**
     * Default page size when not specified in request.
     * Default value: 20
     */
    private int defaultPageSize = 20;

    /**
     * Maximum allowed page size to prevent excessive data retrieval.
     * Default value: 100
     */
    private int maxPageSize = 100;

    /**
     * Get the effective page size, ensuring it doesn't exceed the maximum.
     *
     * @param requestedSize the requested page size, or null for default
     * @return the effective page size (between 1 and maxPageSize)
     */
    public int getEffectivePageSize(Integer requestedSize) {
        if (requestedSize == null || requestedSize <= 0) {
            return defaultPageSize;
        }
        return Math.min(requestedSize, maxPageSize);
    }
}

