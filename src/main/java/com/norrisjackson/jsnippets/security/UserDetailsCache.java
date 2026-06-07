package com.norrisjackson.jsnippets.security;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Caffeine-backed cache for UserDetails used by JwtAuthenticationFilter.
 * Avoids hitting the database on every authenticated API request.
 * Entries expire after a configurable TTL (default 5 minutes).
 */
@Component
public class UserDetailsCache {

    private final Cache<String, UserDetails> cache;

    public UserDetailsCache(@Value("${user-details.cache.ttl-minutes:5}") long ttlMinutes) {
        this.cache = Caffeine.newBuilder()
                .expireAfterWrite(ttlMinutes, TimeUnit.MINUTES)
                .maximumSize(10000)
                .build();
    }

    public UserDetails get(String username) {
        return cache.getIfPresent(username);
    }

    public void put(String username, UserDetails userDetails) {
        cache.put(username, userDetails);
    }

    long estimatedSize() {
        return cache.estimatedSize();
    }
}
