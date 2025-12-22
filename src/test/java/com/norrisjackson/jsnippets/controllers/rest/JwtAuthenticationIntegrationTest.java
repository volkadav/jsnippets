package com.norrisjackson.jsnippets.controllers.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.norrisjackson.jsnippets.data.Snippet;
import com.norrisjackson.jsnippets.data.SnippetRepository;
import com.norrisjackson.jsnippets.data.User;
import com.norrisjackson.jsnippets.data.UserRepository;
import com.norrisjackson.jsnippets.security.dto.AuthenticationRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for JWT authentication in the Snippets API.
 * Tests complete JWT authentication flow with actual API endpoints.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class JwtAuthenticationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SnippetRepository snippetRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    private User testUser;
    private User otherUser;
    private Snippet testSnippet;

    private static final String TEST_USERNAME = "jwtuser";
    private static final String TEST_PASSWORD = "jwtpass123";
    private static final String OTHER_USERNAME = "otheruser";
    private static final String OTHER_PASSWORD = "otherpass456";

    @BeforeEach
    void setUp() {
        // Clean up
        snippetRepository.deleteAll();
        userRepository.deleteAll();

        // Create test users
        testUser = new User();
        testUser.setUsername(TEST_USERNAME);
        testUser.setEmail("jwtuser@example.com");
        testUser.setPasswordHash(passwordEncoder.encode(TEST_PASSWORD));
        testUser.setCreatedAt(new Date());
        testUser = userRepository.save(testUser);

        otherUser = new User();
        otherUser.setUsername(OTHER_USERNAME);
        otherUser.setEmail("other@example.com");
        otherUser.setPasswordHash(passwordEncoder.encode(OTHER_PASSWORD));
        otherUser.setCreatedAt(new Date());
        otherUser = userRepository.save(otherUser);

        // Create test snippet
        testSnippet = new Snippet();
        testSnippet.setContents("Test snippet for JWT auth");
        testSnippet.setPoster(testUser);
        testSnippet.setCreatedAt(new Date());
        testSnippet.setEditedAt(new Date());
        testSnippet = snippetRepository.save(testSnippet);
    }

    /**
     * Helper method to get JWT token for a user
     */
    private String getJwtToken(String username, String password) throws Exception {
        AuthenticationRequest request = new AuthenticationRequest(username, password);

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        String response = result.getResponse().getContentAsString();
        @SuppressWarnings("unchecked")
        Map<String, Object> responseMap = objectMapper.readValue(response, Map.class);
        return (String) responseMap.get("token");
    }

    // ==================== Authentication Tests ====================

    @Test
    void login_WithValidCredentials_ReturnsJwtToken() throws Exception {
        AuthenticationRequest request = new AuthenticationRequest(TEST_USERNAME, TEST_PASSWORD);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.token").isString())
                .andExpect(jsonPath("$.tokenType", is("Bearer")))
                .andExpect(jsonPath("$.username", is(TEST_USERNAME)))
                .andExpect(jsonPath("$.expiresIn").isNumber())
                .andExpect(jsonPath("$.expiresIn", greaterThan(0)));
    }

    @Test
    void login_WithInvalidUsername_ReturnsUnauthorized() throws Exception {
        AuthenticationRequest request = new AuthenticationRequest("nonexistent", TEST_PASSWORD);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error", is("Invalid username or password")));
    }

    @Test
    void login_WithInvalidPassword_ReturnsUnauthorized() throws Exception {
        AuthenticationRequest request = new AuthenticationRequest(TEST_USERNAME, "wrongpassword");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error", is("Invalid username or password")));
    }

    @Test
    void validateToken_WithValidToken_ReturnsValid() throws Exception {
        String token = getJwtToken(TEST_USERNAME, TEST_PASSWORD);

        mockMvc.perform(get("/api/auth/validate")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid", is(true)))
                .andExpect(jsonPath("$.username", is(TEST_USERNAME)))
                .andExpect(jsonPath("$.expiresAt").exists());
    }

    @Test
    void validateToken_WithInvalidToken_ReturnsInvalid() throws Exception {
        mockMvc.perform(get("/api/auth/validate")
                        .header("Authorization", "Bearer invalid.token.here"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid", is(false)))
                .andExpect(jsonPath("$.username").doesNotExist());
    }

    @Test
    void validateToken_WithoutAuthHeader_ReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/auth/validate"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("Missing or invalid Authorization header")));
    }

    // ==================== JWT with Snippets API Tests ====================

    @Test
    void getSnippets_WithValidJwtToken_ReturnsSnippets() throws Exception {
        String token = getJwtToken(TEST_USERNAME, TEST_PASSWORD);

        mockMvc.perform(get("/api/snippets")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].contents", is("Test snippet for JWT auth")));
    }

    @Test
    void getSnippets_WithoutJwtToken_ReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/snippets"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getSnippets_WithInvalidJwtToken_ReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/snippets")
                        .header("Authorization", "Bearer invalid.token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getSnippetById_WithValidJwtToken_ReturnsSnippet() throws Exception {
        String token = getJwtToken(TEST_USERNAME, TEST_PASSWORD);

        mockMvc.perform(get("/api/snippets/" + testSnippet.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(testSnippet.getId().intValue())))
                .andExpect(jsonPath("$.contents", is("Test snippet for JWT auth")));
    }

    @Test
    void createSnippet_WithValidJwtToken_CreatesSnippet() throws Exception {
        String token = getJwtToken(TEST_USERNAME, TEST_PASSWORD);
        String newContent = "New snippet via JWT";

        mockMvc.perform(post("/api/snippets")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("contents", newContent))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.contents", is(newContent)))
                .andExpect(jsonPath("$.poster.username", is(TEST_USERNAME)));
    }

    @Test
    void createSnippet_WithoutJwtToken_ReturnsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/snippets")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("contents", "New snippet"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void updateSnippet_WithValidJwtToken_UpdatesSnippet() throws Exception {
        String token = getJwtToken(TEST_USERNAME, TEST_PASSWORD);
        String updatedContent = "Updated via JWT";
        Map<String, String> updates = new HashMap<>();
        updates.put("contents", updatedContent);

        mockMvc.perform(patch("/api/snippets/" + testSnippet.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updates)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contents", is(updatedContent)));
    }

    @Test
    void updateSnippet_WithoutJwtToken_ReturnsUnauthorized() throws Exception {
        Map<String, String> updates = new HashMap<>();
        updates.put("contents", "Updated content");

        mockMvc.perform(patch("/api/snippets/" + testSnippet.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updates)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deleteSnippet_WithValidJwtToken_DeletesSnippet() throws Exception {
        String token = getJwtToken(TEST_USERNAME, TEST_PASSWORD);

        mockMvc.perform(delete("/api/snippets/" + testSnippet.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        // Verify deletion
        mockMvc.perform(get("/api/snippets/" + testSnippet.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteSnippet_WithoutJwtToken_ReturnsUnauthorized() throws Exception {
        mockMvc.perform(delete("/api/snippets/" + testSnippet.getId()))
                .andExpect(status().isUnauthorized());
    }

    // ==================== Multi-user JWT Tests ====================

    @Test
    void twoUsers_WithDifferentTokens_CannotAccessEachOthersSnippets() throws Exception {
        String testUserToken = getJwtToken(TEST_USERNAME, TEST_PASSWORD);
        String otherUserToken = getJwtToken(OTHER_USERNAME, OTHER_PASSWORD);

        // testUser can access their own snippet
        mockMvc.perform(get("/api/snippets/" + testSnippet.getId())
                        .header("Authorization", "Bearer " + testUserToken))
                .andExpect(status().isOk());

        // otherUser cannot access testUser's snippet
        mockMvc.perform(get("/api/snippets/" + testSnippet.getId())
                        .header("Authorization", "Bearer " + otherUserToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void userCannotUseOthersToken_ToAccessTheirSnippets() throws Exception {
        String otherUserToken = getJwtToken(OTHER_USERNAME, OTHER_PASSWORD);

        // otherUser's token should not give access to testUser's snippets
        mockMvc.perform(get("/api/snippets/" + testSnippet.getId())
                        .header("Authorization", "Bearer " + otherUserToken))
                .andExpect(status().isNotFound());
    }

    // ==================== Complete JWT Workflow Tests ====================

    @Test
    void completeJwtWorkflow_LoginCreateReadUpdateDelete() throws Exception {
        // 1. Login and get token
        String token = getJwtToken(TEST_USERNAME, TEST_PASSWORD);
        assertNotNull(token);
        assertFalse(token.isEmpty());

        // 2. Create snippet with token
        String initialContent = "JWT workflow snippet";
        MvcResult createResult = mockMvc.perform(post("/api/snippets")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("contents", initialContent))
                .andExpect(status().isCreated())
                .andReturn();

        Snippet created = objectMapper.readValue(createResult.getResponse().getContentAsString(), Snippet.class);
        Long snippetId = created.getId();

        // 3. Read snippet with token
        mockMvc.perform(get("/api/snippets/" + snippetId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contents", is(initialContent)));

        // 4. Update snippet with token
        String updatedContent = "Updated JWT workflow snippet";
        Map<String, String> updates = new HashMap<>();
        updates.put("contents", updatedContent);

        mockMvc.perform(patch("/api/snippets/" + snippetId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updates)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contents", is(updatedContent)));

        // 5. Delete snippet with token
        mockMvc.perform(delete("/api/snippets/" + snippetId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        // 6. Verify deletion
        mockMvc.perform(get("/api/snippets/" + snippetId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void jwtToken_WorksAcrossMultipleRequests() throws Exception {
        String token = getJwtToken(TEST_USERNAME, TEST_PASSWORD);

        // Use the same token for multiple requests
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(get("/api/snippets")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk());
        }
    }

    @Test
    void malformedAuthorizationHeader_ReturnsUnauthorized() throws Exception {
        // Missing "Bearer " prefix
        mockMvc.perform(get("/api/snippets")
                        .header("Authorization", "sometoken"))
                .andExpect(status().isUnauthorized());

        // Wrong prefix
        mockMvc.perform(get("/api/snippets")
                        .header("Authorization", "Basic sometoken"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void emptyAuthorizationHeader_ReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/snippets")
                        .header("Authorization", ""))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void jwtTokenInBody_DoesNotAuthenticate() throws Exception {
        String token = getJwtToken(TEST_USERNAME, TEST_PASSWORD);

        // Token should be in header, not body
        mockMvc.perform(post("/api/snippets")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("contents", "New snippet")
                        .param("token", token)) // Wrong - token should be in header
                .andExpect(status().isUnauthorized());
    }

    @Test
    void differentUsersTokens_AreUnique() throws Exception {
        String token1 = getJwtToken(TEST_USERNAME, TEST_PASSWORD);
        String token2 = getJwtToken(OTHER_USERNAME, OTHER_PASSWORD);

        assertNotEquals(token1, token2);

        // Each token works for its own user
        mockMvc.perform(get("/api/snippets")
                        .header("Authorization", "Bearer " + token1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].poster.username", is(TEST_USERNAME)));

        mockMvc.perform(get("/api/snippets")
                        .header("Authorization", "Bearer " + token2))
                .andExpect(status().isOk());
    }
}

