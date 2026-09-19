package com.norrisjackson.jsnippets.configs;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.data.redis.autoconfigure.DataRedisAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;

/**
 * Configuration to enable Redis-based HTTP session storage.
 * Only activated when app.session.store-type=redis is set.
 *
 * When not using Redis, Spring Session falls back to the default
 * in-memory session storage (suitable for single-server deployments).
 *
 * Note: Spring Boot 4 removed the {@code spring.session.store-type} property, so
 * this project now uses its own {@code app.session.store-type} flag to gate the
 * optional Redis session infrastructure.
 */
@Configuration
@ConditionalOnProperty(name = "app.session.store-type", havingValue = "redis")
@EnableRedisHttpSession
@Import(DataRedisAutoConfiguration.class)
public class RedisSessionConfig {
    // Redis session configuration is handled by Spring Boot auto-configuration
    // based on the properties in application.properties
}

