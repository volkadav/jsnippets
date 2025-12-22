package com.norrisjackson.jsnippets.configs;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for PaginationConfig to verify configuration loading and effective page size calculation.
 */
@SpringBootTest
@ActiveProfiles("test")
class PaginationConfigTest {

    @Autowired
    private PaginationConfig paginationConfig;

    @Test
    void configurationLoadsFromProperties() {
        // Verify configuration is loaded from application-test.properties
        assertEquals(20, paginationConfig.getDefaultPageSize());
        assertEquals(100, paginationConfig.getMaxPageSize());
    }

    @Test
    void getEffectivePageSize_WithNull_ReturnsDefault() {
        int result = paginationConfig.getEffectivePageSize(null);
        assertEquals(20, result);
    }

    @Test
    void getEffectivePageSize_WithZero_ReturnsDefault() {
        int result = paginationConfig.getEffectivePageSize(0);
        assertEquals(20, result);
    }

    @Test
    void getEffectivePageSize_WithNegative_ReturnsDefault() {
        int result = paginationConfig.getEffectivePageSize(-5);
        assertEquals(20, result);
    }

    @Test
    void getEffectivePageSize_WithValidSize_ReturnsRequestedSize() {
        int result = paginationConfig.getEffectivePageSize(10);
        assertEquals(10, result);
    }

    @Test
    void getEffectivePageSize_WithMaxSize_ReturnsMaxSize() {
        int result = paginationConfig.getEffectivePageSize(100);
        assertEquals(100, result);
    }

    @Test
    void getEffectivePageSize_ExceedingMax_ReturnsMaxSize() {
        int result = paginationConfig.getEffectivePageSize(150);
        assertEquals(100, result);
    }

    @Test
    void getEffectivePageSize_VeryLargeNumber_ReturnsMaxSize() {
        int result = paginationConfig.getEffectivePageSize(Integer.MAX_VALUE);
        assertEquals(100, result);
    }
}

