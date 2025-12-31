package com.norrisjackson.jsnippets.configs;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;

/**
 * Configuration to enable Redis-based HTTP session storage.
 * Only activated when spring.session.store-type=redis is set.
 *
 * When not using Redis, Spring Session falls back to the default
 * in-memory session storage (suitable for single-server deployments).
 */
@Configuration
@ConditionalOnProperty(name = "spring.session.store-type", havingValue = "redis")
@EnableRedisHttpSession
@Import(RedisAutoConfiguration.class)
public class RedisSessionConfig {
    // Redis session configuration is handled by Spring Boot auto-configuration
    // based on the properties in application.properties
}

