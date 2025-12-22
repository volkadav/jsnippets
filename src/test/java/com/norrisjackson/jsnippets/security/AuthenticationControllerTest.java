package com.norrisjackson.jsnippets.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.norrisjackson.jsnippets.controllers.rest.AuthenticationController;
import com.norrisjackson.jsnippets.security.dto.AuthenticationRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.ArrayList;
import java.util.Date;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for AuthenticationController.
 * Tests authentication endpoints with mocked dependencies.
 * Uses standalone MockMvc setup to avoid Spring context loading issues with JPA.
 */
@ExtendWith(MockitoExtension.class)
class AuthenticationControllerTest {

    private MockMvc mockMvc;

    private ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private AuthenticationController authenticationController;

    private static final String TEST_USERNAME = "testuser";
    private static final String TEST_PASSWORD = "password123";
    private static final String TEST_TOKEN = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.test.token";

    @BeforeEach
    void setUp() {
        // Set up MockMvc with standalone configuration (no Spring context)
        mockMvc = MockMvcBuilders.standaloneSetup(authenticationController).build();

        // Inject the jwtExpiration value using reflection since @Value won't work in unit tests
        ReflectionTestUtils.setField(authenticationController, "jwtExpiration", 86400000L);
    }

    @Test
    void login_WithValidCredentials_ReturnsToken() throws Exception {
        // Arrange
        AuthenticationRequest request = new AuthenticationRequest(TEST_USERNAME, TEST_PASSWORD);
        UserDetails userDetails = User.builder()
                .username(TEST_USERNAME)
                .password(TEST_PASSWORD)
                .authorities(new ArrayList<>())
                .build();

        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(userDetails);

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(jwtUtil.generateToken(userDetails)).thenReturn(TEST_TOKEN);

        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", is(TEST_TOKEN)))
                .andExpect(jsonPath("$.tokenType", is("Bearer")))
                .andExpect(jsonPath("$.username", is(TEST_USERNAME)))
                .andExpect(jsonPath("$.expiresIn", is(86400000)));

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(jwtUtil).generateToken(userDetails);
    }

    @Test
    void login_WithInvalidCredentials_ReturnsUnauthorized() throws Exception {
        // Arrange
        AuthenticationRequest request = new AuthenticationRequest(TEST_USERNAME, "wrongpassword");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error", is("Invalid username or password")));

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(jwtUtil, never()).generateToken(any());
    }

    @Test
    void login_WithMissingUsername_ReturnsBadRequest() throws Exception {
        // Arrange - missing username
        String requestJson = "{\"password\":\"password123\"}";

        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest());

        verify(authenticationManager, never()).authenticate(any());
    }

    @Test
    void login_WithMissingPassword_ReturnsBadRequest() throws Exception {
        // Arrange - missing password
        String requestJson = "{\"username\":\"testuser\"}";

        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest());

        verify(authenticationManager, never()).authenticate(any());
    }

    @Test
    void login_WithBlankUsername_ReturnsBadRequest() throws Exception {
        // Arrange
        AuthenticationRequest request = new AuthenticationRequest("", TEST_PASSWORD);

        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(authenticationManager, never()).authenticate(any());
    }

    @Test
    void login_WithBlankPassword_ReturnsBadRequest() throws Exception {
        // Arrange
        AuthenticationRequest request = new AuthenticationRequest(TEST_USERNAME, "");

        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(authenticationManager, never()).authenticate(any());
    }

    @Test
    void login_WithEmptyBody_ReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        verify(authenticationManager, never()).authenticate(any());
    }

    @Test
    void login_WithAuthenticationException_ReturnsInternalServerError() throws Exception {
        // Arrange
        AuthenticationRequest request = new AuthenticationRequest(TEST_USERNAME, TEST_PASSWORD);

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new RuntimeException("Unexpected error"));

        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error", is("Authentication failed")));
    }

    @Test
    void validateToken_WithValidToken_ReturnsValid() throws Exception {
        // Arrange
        Date futureDate = new Date(System.currentTimeMillis() + 3600000);
        when(jwtUtil.validateToken(TEST_TOKEN)).thenReturn(true);
        when(jwtUtil.extractUsername(TEST_TOKEN)).thenReturn(TEST_USERNAME);
        when(jwtUtil.extractExpiration(TEST_TOKEN)).thenReturn(futureDate);

        // Act & Assert
        mockMvc.perform(get("/api/auth/validate")
                        .header("Authorization", "Bearer " + TEST_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid", is(true)))
                .andExpect(jsonPath("$.username", is(TEST_USERNAME)))
                .andExpect(jsonPath("$.expiresAt").exists());

        verify(jwtUtil).validateToken(TEST_TOKEN);
        verify(jwtUtil).extractUsername(TEST_TOKEN);
        verify(jwtUtil).extractExpiration(TEST_TOKEN);
    }

    @Test
    void validateToken_WithInvalidToken_ReturnsInvalid() throws Exception {
        // Arrange
        String invalidToken = "invalid.token";
        when(jwtUtil.validateToken(invalidToken)).thenReturn(false);

        // Act & Assert
        mockMvc.perform(get("/api/auth/validate")
                        .header("Authorization", "Bearer " + invalidToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid", is(false)))
                .andExpect(jsonPath("$.username").doesNotExist());

        verify(jwtUtil).validateToken(invalidToken);
        verify(jwtUtil, never()).extractUsername(anyString());
    }

    @Test
    void validateToken_WithoutAuthorizationHeader_ReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/auth/validate"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("Missing or invalid Authorization header")));

        verify(jwtUtil, never()).validateToken(anyString());
    }

    @Test
    void validateToken_WithInvalidHeaderFormat_ReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/auth/validate")
                        .header("Authorization", "InvalidFormat " + TEST_TOKEN))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("Missing or invalid Authorization header")));

        verify(jwtUtil, never()).validateToken(anyString());
    }

    @Test
    void validateToken_WithMissingBearer_ReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/auth/validate")
                        .header("Authorization", TEST_TOKEN))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("Missing or invalid Authorization header")));

        verify(jwtUtil, never()).validateToken(anyString());
    }

    @Test
    void validateToken_WithTokenValidationException_ReturnsUnauthorized() throws Exception {
        // Arrange
        when(jwtUtil.validateToken(anyString())).thenThrow(new RuntimeException("Token validation error"));

        // Act & Assert
        mockMvc.perform(get("/api/auth/validate")
                        .header("Authorization", "Bearer " + TEST_TOKEN))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error", is("Token validation failed")));
    }
}
