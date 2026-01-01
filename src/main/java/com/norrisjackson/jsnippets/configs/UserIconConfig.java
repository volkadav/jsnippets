package com.norrisjackson.jsnippets.configs;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Configuration properties for user icon/avatar settings.
 */
@Configuration
@ConfigurationProperties(prefix = "user.icon")
@Getter
@Setter
public class UserIconConfig {

    /**
     * Maximum size of uploaded user icons in bytes.
     * Default: 32KB (32768 bytes)
     */
    private int maxSize = 32 * 1024;

    /**
     * Allowed content types for user icon uploads.
     */
    private List<String> allowedContentTypes = List.of(
            "image/png", "image/jpeg", "image/gif", "image/webp"
    );

    /**
     * Size in pixels for thumbnail icons (used in snippet listings, etc).
     * Default: 32px
     */
    private int thumbnailSize = 32;

    /**
     * Size in pixels for full-size icons (used on profile pages).
     * Default: 128px
     */
    private int fullSize = 128;
}

