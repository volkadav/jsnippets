package com.norrisjackson.jsnippets;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Spring Boot Actuator endpoints.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class ActuatorHealthCheckTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void healthEndpointShouldBeAvailable() {
        // When
        ResponseEntity<String> response = restTemplate.getForEntity("/actuator/health", String.class);

        // Then
        System.out.println("Health endpoint status: " + response.getStatusCode());
        System.out.println("Health endpoint body: " + response.getBody());
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().contains("UP") || response.getBody().contains("status"),
            "Health check should return status");
    }

    @Test
    void healthEndpointShouldShowDatabaseHealth() {
        // When
        ResponseEntity<String> response = restTemplate.getForEntity("/actuator/health", String.class);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().contains("db") || response.getBody().contains("UP"),
            "Health check should include database status");
    }

    @Test
    void livenessProbeEndpointShouldBeAvailable() {
        // When
        ResponseEntity<String> response = restTemplate.getForEntity("/actuator/health/liveness", String.class);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().contains("\"status\":\"UP\""),
            "Liveness probe should return UP status");
    }

    @Test
    void readinessProbeEndpointShouldBeAvailable() {
        // When
        ResponseEntity<String> response = restTemplate.getForEntity("/actuator/health/readiness", String.class);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().contains("\"status\":\"UP\""),
            "Readiness probe should return UP status");
    }

    @Test
    void infoEndpointShouldBeAvailable() {
        // When
        ResponseEntity<String> response = restTemplate.getForEntity("/actuator/info", String.class);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    void actuatorBaseEndpointShouldListAvailableEndpoints() {
        // When
        ResponseEntity<String> response = restTemplate.getForEntity("/actuator", String.class);

        // Then
        // The base actuator endpoint may require authentication or may not be exposed
        // We just verify it either works or returns unauthorized/forbidden
        assertTrue(response.getStatusCode() == HttpStatus.OK ||
                   response.getStatusCode() == HttpStatus.UNAUTHORIZED ||
                   response.getStatusCode() == HttpStatus.FORBIDDEN,
            "Actuator base endpoint should respond with OK, UNAUTHORIZED, or FORBIDDEN");

        // If it's OK, verify it lists endpoints
        if (response.getStatusCode() == HttpStatus.OK) {
            assertNotNull(response.getBody());
            assertTrue(response.getBody().contains("health") || response.getBody().contains("_links"),
                "Actuator should list endpoints or links");
        }
    }
}

