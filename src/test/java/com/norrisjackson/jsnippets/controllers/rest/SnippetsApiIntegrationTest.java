package com.norrisjackson.jsnippets.controllers.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.norrisjackson.jsnippets.data.Snippet;
import com.norrisjackson.jsnippets.data.SnippetRepository;
import com.norrisjackson.jsnippets.data.User;
import com.norrisjackson.jsnippets.data.UserRepository;
import com.norrisjackson.jsnippets.controllers.rest.dto.AuthenticationRequest;
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
 * Integration tests for the Snippets API REST controller.
 * Tests all CRUD operations, authentication/authorization scenarios, and pagination
 * using JWT token-based authentication.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class SnippetsApiIntegrationTest {

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

    // API paths (versioned)
    private static final String AUTH_LOGIN_PATH = "/api/v1/auth/login";
    private static final String AUTH_VALIDATE_PATH = "/api/v1/auth/validate";
    private static final String SNIPPETS_PATH = "/api/v1/snippets";

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

        MvcResult result = mockMvc.perform(post(AUTH_LOGIN_PATH)
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

        mockMvc.perform(post(AUTH_LOGIN_PATH)
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

        mockMvc.perform(post(AUTH_LOGIN_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code", is("AUTH_INVALID_CREDENTIALS")))
                .andExpect(jsonPath("$.message", is("Invalid username or password")));
    }

    @Test
    void login_WithInvalidPassword_ReturnsUnauthorized() throws Exception {
        AuthenticationRequest request = new AuthenticationRequest(TEST_USERNAME, "wrongpassword");

        mockMvc.perform(post(AUTH_LOGIN_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code", is("AUTH_INVALID_CREDENTIALS")))
                .andExpect(jsonPath("$.message", is("Invalid username or password")));
    }

    @Test
    void validateToken_WithValidToken_ReturnsValid() throws Exception {
        String token = getJwtToken(TEST_USERNAME, TEST_PASSWORD);

        mockMvc.perform(get(AUTH_VALIDATE_PATH)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid", is(true)))
                .andExpect(jsonPath("$.username", is(TEST_USERNAME)))
                .andExpect(jsonPath("$.expiresAt").exists());
    }

    @Test
    void validateToken_WithInvalidToken_ReturnsInvalid() throws Exception {
        mockMvc.perform(get(AUTH_VALIDATE_PATH)
                        .header("Authorization", "Bearer invalid.token.here"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid", is(false)))
                .andExpect(jsonPath("$.username").doesNotExist());
    }

    @Test
    void validateToken_WithoutAuthHeader_ReturnsBadRequest() throws Exception {
        mockMvc.perform(get(AUTH_VALIDATE_PATH))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("AUTH_TOKEN_MISSING")))
                .andExpect(jsonPath("$.message", is("Missing or invalid Authorization header")));
    }

    // ==================== JWT with Snippets API Tests ====================

    @Test
    void getSnippets_WithValidJwtToken_ReturnsSnippets() throws Exception {
        String token = getJwtToken(TEST_USERNAME, TEST_PASSWORD);

        mockMvc.perform(get(SNIPPETS_PATH)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].contents", is("Test snippet for JWT auth")));
    }

    @Test
    void getSnippets_WithoutJwtToken_ReturnsUnauthorized() throws Exception {
        mockMvc.perform(get(SNIPPETS_PATH))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getSnippets_WithInvalidJwtToken_ReturnsUnauthorized() throws Exception {
        mockMvc.perform(get(SNIPPETS_PATH)
                        .header("Authorization", "Bearer invalid.token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getSnippetById_WithValidJwtToken_ReturnsSnippet() throws Exception {
        String token = getJwtToken(TEST_USERNAME, TEST_PASSWORD);

        mockMvc.perform(get(SNIPPETS_PATH + "/" + testSnippet.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(testSnippet.getId().intValue())))
                .andExpect(jsonPath("$.contents", is("Test snippet for JWT auth")));
    }

    @Test
    void createSnippet_WithValidJwtToken_CreatesSnippet() throws Exception {
        String token = getJwtToken(TEST_USERNAME, TEST_PASSWORD);
        String newContent = "New snippet via JWT";

        mockMvc.perform(post(SNIPPETS_PATH)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("contents", newContent))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.contents", is(newContent)))
                .andExpect(jsonPath("$.poster.username", is(TEST_USERNAME)));
    }

    @Test
    void createSnippet_WithoutJwtToken_ReturnsUnauthorized() throws Exception {
        mockMvc.perform(post(SNIPPETS_PATH)
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

        mockMvc.perform(patch(SNIPPETS_PATH + "/" + testSnippet.getId())
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

        mockMvc.perform(patch(SNIPPETS_PATH + "/" + testSnippet.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updates)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deleteSnippet_WithValidJwtToken_DeletesSnippet() throws Exception {
        String token = getJwtToken(TEST_USERNAME, TEST_PASSWORD);

        mockMvc.perform(delete(SNIPPETS_PATH + "/" + testSnippet.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        // Verify deletion
        mockMvc.perform(get(SNIPPETS_PATH + "/" + testSnippet.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteSnippet_WithoutJwtToken_ReturnsUnauthorized() throws Exception {
        mockMvc.perform(delete(SNIPPETS_PATH + "/" + testSnippet.getId()))
                .andExpect(status().isUnauthorized());
    }

    // ==================== Multi-user JWT Tests ====================

    @Test
    void twoUsers_WithDifferentTokens_CannotAccessEachOthersSnippets() throws Exception {
        String testUserToken = getJwtToken(TEST_USERNAME, TEST_PASSWORD);
        String otherUserToken = getJwtToken(OTHER_USERNAME, OTHER_PASSWORD);

        // testUser can access their own snippet
        mockMvc.perform(get(SNIPPETS_PATH + "/" + testSnippet.getId())
                        .header("Authorization", "Bearer " + testUserToken))
                .andExpect(status().isOk());

        // otherUser cannot access testUser's snippet
        mockMvc.perform(get(SNIPPETS_PATH + "/" + testSnippet.getId())
                        .header("Authorization", "Bearer " + otherUserToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void userCannotUseOthersToken_ToAccessTheirSnippets() throws Exception {
        String otherUserToken = getJwtToken(OTHER_USERNAME, OTHER_PASSWORD);

        // otherUser's token should not give access to testUser's snippets
        mockMvc.perform(get(SNIPPETS_PATH + "/" + testSnippet.getId())
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
        MvcResult createResult = mockMvc.perform(post(SNIPPETS_PATH)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("contents", initialContent))
                .andExpect(status().isCreated())
                .andReturn();

        Snippet created = objectMapper.readValue(createResult.getResponse().getContentAsString(), Snippet.class);
        Long snippetId = created.getId();

        // 3. Read snippet with token
        mockMvc.perform(get(SNIPPETS_PATH + "/" + snippetId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contents", is(initialContent)));

        // 4. Update snippet with token
        String updatedContent = "Updated JWT workflow snippet";
        Map<String, String> updates = new HashMap<>();
        updates.put("contents", updatedContent);

        mockMvc.perform(patch(SNIPPETS_PATH + "/" + snippetId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updates)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contents", is(updatedContent)));

        // 5. Delete snippet with token
        mockMvc.perform(delete(SNIPPETS_PATH + "/" + snippetId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        // 6. Verify deletion
        mockMvc.perform(get(SNIPPETS_PATH + "/" + snippetId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void jwtToken_WorksAcrossMultipleRequests() throws Exception {
        String token = getJwtToken(TEST_USERNAME, TEST_PASSWORD);

        // Use the same token for multiple requests
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(get(SNIPPETS_PATH)
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk());
        }
    }

    @Test
    void malformedAuthorizationHeader_ReturnsUnauthorized() throws Exception {
        // Missing "Bearer " prefix
        mockMvc.perform(get(SNIPPETS_PATH)
                        .header("Authorization", "sometoken"))
                .andExpect(status().isUnauthorized());

        // Wrong prefix
        mockMvc.perform(get(SNIPPETS_PATH)
                        .header("Authorization", "Basic sometoken"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void emptyAuthorizationHeader_ReturnsUnauthorized() throws Exception {
        mockMvc.perform(get(SNIPPETS_PATH)
                        .header("Authorization", ""))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void jwtTokenInBody_DoesNotAuthenticate() throws Exception {
        String token = getJwtToken(TEST_USERNAME, TEST_PASSWORD);

        // Token should be in header, not body
        mockMvc.perform(post(SNIPPETS_PATH)
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
        mockMvc.perform(get(SNIPPETS_PATH)
                        .header("Authorization", "Bearer " + token1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].poster.username", is(TEST_USERNAME)));

        mockMvc.perform(get(SNIPPETS_PATH)
                        .header("Authorization", "Bearer " + token2))
                .andExpect(status().isOk());
    }

    // ==================== Pagination Tests ====================

    @Test
    void getSnippets_WithPagination_ReturnsPaginatedResults() throws Exception {
        String token = getJwtToken(TEST_USERNAME, TEST_PASSWORD);

        // Create additional snippets
        for (int i = 0; i < 5; i++) {
            Snippet snippet = new Snippet();
            snippet.setContents("Paginated snippet " + i);
            snippet.setPoster(testUser);
            snippet.setCreatedAt(new Date());
            snippet.setEditedAt(new Date());
            snippetRepository.save(snippet);
        }

        mockMvc.perform(get(SNIPPETS_PATH)
                        .param("pageNumber", "0")
                        .param("pageSize", "3")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(3)))
                .andExpect(jsonPath("$.totalElements", is(6)))
                .andExpect(jsonPath("$.totalPages", is(2)))
                .andExpect(jsonPath("$.number", is(0)))
                .andExpect(jsonPath("$.size", is(3)));
    }

    @Test
    void getSnippets_SecondPage_ReturnsCorrectPage() throws Exception {
        String token = getJwtToken(TEST_USERNAME, TEST_PASSWORD);

        // Create additional snippets
        for (int i = 0; i < 5; i++) {
            Snippet snippet = new Snippet();
            snippet.setContents("Paginated snippet " + i);
            snippet.setPoster(testUser);
            snippet.setCreatedAt(new Date());
            snippet.setEditedAt(new Date());
            snippetRepository.save(snippet);
        }

        mockMvc.perform(get(SNIPPETS_PATH)
                        .param("pageNumber", "1")
                        .param("pageSize", "3")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(3)))
                .andExpect(jsonPath("$.number", is(1)));
    }

    // ==================== Validation Tests ====================

    @Test
    void createSnippet_WithEmptyContent_ReturnsBadRequest() throws Exception {
        String token = getJwtToken(TEST_USERNAME, TEST_PASSWORD);

        mockMvc.perform(post(SNIPPETS_PATH)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("contents", ""))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createSnippet_WithBlankContent_ReturnsBadRequest() throws Exception {
        String token = getJwtToken(TEST_USERNAME, TEST_PASSWORD);

        mockMvc.perform(post(SNIPPETS_PATH)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("contents", "   "))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateSnippet_WithEmptyContent_ReturnsBadRequest() throws Exception {
        String token = getJwtToken(TEST_USERNAME, TEST_PASSWORD);
        Map<String, String> updates = new HashMap<>();
        updates.put("contents", "");

        mockMvc.perform(patch(SNIPPETS_PATH + "/" + testSnippet.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updates)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateSnippet_WithBlankContent_ReturnsBadRequest() throws Exception {
        String token = getJwtToken(TEST_USERNAME, TEST_PASSWORD);
        Map<String, String> updates = new HashMap<>();
        updates.put("contents", "   ");

        mockMvc.perform(patch(SNIPPETS_PATH + "/" + testSnippet.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updates)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateSnippet_WithNullContent_ReturnsBadRequest() throws Exception {
        String token = getJwtToken(TEST_USERNAME, TEST_PASSWORD);
        Map<String, String> updates = new HashMap<>();
        // Don't put "contents" key at all

        mockMvc.perform(patch(SNIPPETS_PATH + "/" + testSnippet.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updates)))
                .andExpect(status().isBadRequest());
    }

    // ==================== Authorization Tests ====================

    @Test
    void updateSnippet_OtherUsersSnippet_ReturnsForbidden() throws Exception {
        String testUserToken = getJwtToken(TEST_USERNAME, TEST_PASSWORD);

        // Create a snippet for otherUser
        Snippet otherSnippet = new Snippet();
        otherSnippet.setContents("Other user's snippet");
        otherSnippet.setPoster(otherUser);
        otherSnippet.setCreatedAt(new Date());
        otherSnippet.setEditedAt(new Date());
        otherSnippet = snippetRepository.save(otherSnippet);

        Map<String, String> updates = new HashMap<>();
        updates.put("contents", "Trying to update other's snippet");

        mockMvc.perform(patch(SNIPPETS_PATH + "/" + otherSnippet.getId())
                        .header("Authorization", "Bearer " + testUserToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updates)))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateSnippet_NonExistentSnippet_ReturnsForbidden() throws Exception {
        String token = getJwtToken(TEST_USERNAME, TEST_PASSWORD);
        Map<String, String> updates = new HashMap<>();
        updates.put("contents", "Updated content");

        // Non-existent snippet returns 403 because userOwnsSnippet returns false
        mockMvc.perform(patch(SNIPPETS_PATH + "/99999")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updates)))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteSnippet_OtherUsersSnippet_ReturnsForbidden() throws Exception {
        String testUserToken = getJwtToken(TEST_USERNAME, TEST_PASSWORD);

        // Create a snippet for otherUser
        Snippet otherSnippet = new Snippet();
        otherSnippet.setContents("Other user's snippet to delete");
        otherSnippet.setPoster(otherUser);
        otherSnippet.setCreatedAt(new Date());
        otherSnippet.setEditedAt(new Date());
        otherSnippet = snippetRepository.save(otherSnippet);

        mockMvc.perform(delete(SNIPPETS_PATH + "/" + otherSnippet.getId())
                        .header("Authorization", "Bearer " + testUserToken))
                .andExpect(status().isForbidden());

        // Verify snippet still exists for otherUser
        String otherUserToken = getJwtToken(OTHER_USERNAME, OTHER_PASSWORD);
        mockMvc.perform(get(SNIPPETS_PATH + "/" + otherSnippet.getId())
                        .header("Authorization", "Bearer " + otherUserToken))
                .andExpect(status().isOk());
    }

    @Test
    void deleteSnippet_NonExistentSnippet_ReturnsForbidden() throws Exception {
        String token = getJwtToken(TEST_USERNAME, TEST_PASSWORD);

        // Non-existent snippet returns 403 because userOwnsSnippet returns false
        mockMvc.perform(delete(SNIPPETS_PATH + "/99999")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }
}

