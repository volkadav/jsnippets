package com.norrisjackson.jsnippets.controllers.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.norrisjackson.jsnippets.data.Snippet;
import com.norrisjackson.jsnippets.data.SnippetRepository;
import com.norrisjackson.jsnippets.data.User;
import com.norrisjackson.jsnippets.data.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for the SnippetsApi REST controller.
 * Tests all CRUD operations and authentication/authorization scenarios.
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
    private Snippet otherSnippet;

    private static final String TEST_USERNAME = "testuser";
    private static final String TEST_PASSWORD = "password123";
    private static final String OTHER_USERNAME = "otheruser";
    private static final String OTHER_PASSWORD = "password456";

    @BeforeEach
    void setUp() {
        // Clean up
        snippetRepository.deleteAll();
        userRepository.deleteAll();

        // Create test user
        testUser = new User();
        testUser.setUsername(TEST_USERNAME);
        testUser.setEmail("test@example.com");
        testUser.setPasswordHash(passwordEncoder.encode(TEST_PASSWORD));
        testUser.setCreatedAt(new Date());
        testUser = userRepository.save(testUser);

        // Create other user
        otherUser = new User();
        otherUser.setUsername(OTHER_USERNAME);
        otherUser.setEmail("other@example.com");
        otherUser.setPasswordHash(passwordEncoder.encode(OTHER_PASSWORD));
        otherUser.setCreatedAt(new Date());
        otherUser = userRepository.save(otherUser);

        // Create test snippet for testUser
        testSnippet = new Snippet();
        testSnippet.setContents("Test snippet content");
        testSnippet.setPoster(testUser);
        testSnippet.setCreatedAt(new Date());
        testSnippet.setEditedAt(new Date());
        testSnippet = snippetRepository.save(testSnippet);

        // Create snippet for otherUser
        otherSnippet = new Snippet();
        otherSnippet.setContents("Other user's snippet");
        otherSnippet.setPoster(otherUser);
        otherSnippet.setCreatedAt(new Date());
        otherSnippet.setEditedAt(new Date());
        otherSnippet = snippetRepository.save(otherSnippet);
    }

    // ==================== GET /api/snippets ====================

    @Test
    void getSnippets_WithAuthentication_ReturnsUserSnippets() throws Exception {
        mockMvc.perform(get("/api/snippets")
                        .with(httpBasic(TEST_USERNAME, TEST_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].contents", is("Test snippet content")))
                .andExpect(jsonPath("$.content[0].poster.username", is(TEST_USERNAME)));
    }

    @Test
    void getSnippets_WithoutAuthentication_ReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/snippets"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getSnippets_WithPagination_ReturnsPaginatedResults() throws Exception {
        // Create additional snippets
        for (int i = 0; i < 5; i++) {
            Snippet snippet = new Snippet();
            snippet.setContents("Snippet " + i);
            snippet.setPoster(testUser);
            snippet.setCreatedAt(new Date());
            snippet.setEditedAt(new Date());
            snippetRepository.save(snippet);
        }

        mockMvc.perform(get("/api/snippets")
                        .param("pageNumber", "0")
                        .param("pageSize", "3")
                        .with(httpBasic(TEST_USERNAME, TEST_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(3)))
                .andExpect(jsonPath("$.totalElements", is(6)))
                .andExpect(jsonPath("$.totalPages", is(2)))
                .andExpect(jsonPath("$.number", is(0)))
                .andExpect(jsonPath("$.size", is(3)));
    }

    @Test
    void getSnippets_SecondPage_ReturnsCorrectPage() throws Exception {
        // Create additional snippets
        for (int i = 0; i < 5; i++) {
            Snippet snippet = new Snippet();
            snippet.setContents("Snippet " + i);
            snippet.setPoster(testUser);
            snippet.setCreatedAt(new Date());
            snippet.setEditedAt(new Date());
            snippetRepository.save(snippet);
        }

        mockMvc.perform(get("/api/snippets")
                        .param("pageNumber", "1")
                        .param("pageSize", "3")
                        .with(httpBasic(TEST_USERNAME, TEST_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(3)))
                .andExpect(jsonPath("$.number", is(1)));
    }

    @Test
    void getSnippets_OnlyReturnsOwnSnippets_NotOtherUsers() throws Exception {
        mockMvc.perform(get("/api/snippets")
                        .with(httpBasic(TEST_USERNAME, TEST_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].poster.username", is(TEST_USERNAME)));
    }

    // ==================== GET /api/snippets/{snippetId} ====================

    @Test
    void getSnippetById_OwnSnippet_ReturnsSnippet() throws Exception {
        mockMvc.perform(get("/api/snippets/" + testSnippet.getId())
                        .with(httpBasic(TEST_USERNAME, TEST_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(testSnippet.getId().intValue())))
                .andExpect(jsonPath("$.contents", is("Test snippet content")))
                .andExpect(jsonPath("$.poster.username", is(TEST_USERNAME)));
    }

    @Test
    void getSnippetById_OtherUsersSnippet_ReturnsNotFound() throws Exception {
        mockMvc.perform(get("/api/snippets/" + otherSnippet.getId())
                        .with(httpBasic(TEST_USERNAME, TEST_PASSWORD)))
                .andExpect(status().isNotFound());
    }

    @Test
    void getSnippetById_NonExistentSnippet_ReturnsNotFound() throws Exception {
        mockMvc.perform(get("/api/snippets/99999")
                        .with(httpBasic(TEST_USERNAME, TEST_PASSWORD)))
                .andExpect(status().isNotFound());
    }

    @Test
    void getSnippetById_WithoutAuthentication_ReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/snippets/" + testSnippet.getId()))
                .andExpect(status().isUnauthorized());
    }

    // ==================== POST /api/snippets ====================

    @Test
    void createSnippet_WithValidContent_CreatesSnippet() throws Exception {
        String newContent = "This is a new snippet";

        mockMvc.perform(post("/api/snippets")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("contents", newContent)
                        .with(httpBasic(TEST_USERNAME, TEST_PASSWORD)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.contents", is(newContent)))
                .andExpect(jsonPath("$.poster.username", is(TEST_USERNAME)))
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.editedAt").exists());
    }

    @Test
    void createSnippet_WithEmptyContent_ReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/snippets")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("contents", "")
                        .with(httpBasic(TEST_USERNAME, TEST_PASSWORD)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createSnippet_WithBlankContent_ReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/snippets")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("contents", "   ")
                        .with(httpBasic(TEST_USERNAME, TEST_PASSWORD)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createSnippet_WithoutAuthentication_ReturnsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/snippets")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("contents", "New snippet"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createSnippet_WithInvalidCredentials_ReturnsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/snippets")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("contents", "New snippet")
                        .with(httpBasic(TEST_USERNAME, "wrongpassword")))
                .andExpect(status().isUnauthorized());
    }

    // ==================== PATCH /api/snippets/{snippetId} ====================

    @Test
    void updateSnippet_OwnSnippet_UpdatesSuccessfully() throws Exception {
        String updatedContent = "Updated snippet content";
        Map<String, String> updates = new HashMap<>();
        updates.put("contents", updatedContent);

        mockMvc.perform(patch("/api/snippets/" + testSnippet.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updates))
                        .with(httpBasic(TEST_USERNAME, TEST_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contents", is(updatedContent)))
                .andExpect(jsonPath("$.id", is(testSnippet.getId().intValue())));
    }

    @Test
    void updateSnippet_OtherUsersSnippet_ReturnsForbidden() throws Exception {
        Map<String, String> updates = new HashMap<>();
        updates.put("contents", "Trying to update other's snippet");

        mockMvc.perform(patch("/api/snippets/" + otherSnippet.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updates))
                        .with(httpBasic(TEST_USERNAME, TEST_PASSWORD)))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateSnippet_WithEmptyContent_ReturnsBadRequest() throws Exception {
        Map<String, String> updates = new HashMap<>();
        updates.put("contents", "");

        mockMvc.perform(patch("/api/snippets/" + testSnippet.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updates))
                        .with(httpBasic(TEST_USERNAME, TEST_PASSWORD)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateSnippet_WithBlankContent_ReturnsBadRequest() throws Exception {
        Map<String, String> updates = new HashMap<>();
        updates.put("contents", "   ");

        mockMvc.perform(patch("/api/snippets/" + testSnippet.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updates))
                        .with(httpBasic(TEST_USERNAME, TEST_PASSWORD)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateSnippet_WithNullContent_ReturnsBadRequest() throws Exception {
        Map<String, String> updates = new HashMap<>();
        // Don't put "contents" key at all

        mockMvc.perform(patch("/api/snippets/" + testSnippet.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updates))
                        .with(httpBasic(TEST_USERNAME, TEST_PASSWORD)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateSnippet_NonExistentSnippet_ReturnsForbidden() throws Exception {
        // The API checks ownership first, so non-existent snippet returns 403 (Forbidden)
        // rather than 404 (Not Found) because userOwnsSnippet returns false
        Map<String, String> updates = new HashMap<>();
        updates.put("contents", "Updated content");

        mockMvc.perform(patch("/api/snippets/99999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updates))
                        .with(httpBasic(TEST_USERNAME, TEST_PASSWORD)))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateSnippet_WithoutAuthentication_ReturnsUnauthorized() throws Exception {
        Map<String, String> updates = new HashMap<>();
        updates.put("contents", "Updated content");

        mockMvc.perform(patch("/api/snippets/" + testSnippet.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updates)))
                .andExpect(status().isUnauthorized());
    }

    // ==================== DELETE /api/snippets/{snippetId} ====================

    @Test
    void deleteSnippet_OwnSnippet_DeletesSuccessfully() throws Exception {
        Long snippetId = testSnippet.getId();

        mockMvc.perform(delete("/api/snippets/" + snippetId)
                        .with(httpBasic(TEST_USERNAME, TEST_PASSWORD)))
                .andExpect(status().isNoContent());

        // Verify snippet is deleted
        mockMvc.perform(get("/api/snippets/" + snippetId)
                        .with(httpBasic(TEST_USERNAME, TEST_PASSWORD)))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteSnippet_OtherUsersSnippet_ReturnsForbidden() throws Exception {
        mockMvc.perform(delete("/api/snippets/" + otherSnippet.getId())
                        .with(httpBasic(TEST_USERNAME, TEST_PASSWORD)))
                .andExpect(status().isForbidden());

        // Verify snippet still exists
        mockMvc.perform(get("/api/snippets/" + otherSnippet.getId())
                        .with(httpBasic(OTHER_USERNAME, OTHER_PASSWORD)))
                .andExpect(status().isOk());
    }

    @Test
    void deleteSnippet_NonExistentSnippet_ReturnsForbidden() throws Exception {
        // The API checks ownership first, so non-existent snippet returns 403 (Forbidden)
        // rather than 404 (Not Found) because userOwnsSnippet returns false
        mockMvc.perform(delete("/api/snippets/99999")
                        .with(httpBasic(TEST_USERNAME, TEST_PASSWORD)))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteSnippet_WithoutAuthentication_ReturnsUnauthorized() throws Exception {
        mockMvc.perform(delete("/api/snippets/" + testSnippet.getId()))
                .andExpect(status().isUnauthorized());
    }

    // ==================== Complete CRUD Workflow Tests ====================

    @Test
    void completeCrudWorkflow_CreateReadUpdateDelete() throws Exception {
        // 1. Create
        String initialContent = "Initial content";
        String createResponse = mockMvc.perform(post("/api/snippets")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("contents", initialContent)
                        .with(httpBasic(TEST_USERNAME, TEST_PASSWORD)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.contents", is(initialContent)))
                .andReturn().getResponse().getContentAsString();

        Snippet createdSnippet = objectMapper.readValue(createResponse, Snippet.class);
        Long snippetId = createdSnippet.getId();

        // 2. Read
        mockMvc.perform(get("/api/snippets/" + snippetId)
                        .with(httpBasic(TEST_USERNAME, TEST_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contents", is(initialContent)));

        // 3. Update
        String updatedContent = "Updated content";
        Map<String, String> updates = new HashMap<>();
        updates.put("contents", updatedContent);

        mockMvc.perform(patch("/api/snippets/" + snippetId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updates))
                        .with(httpBasic(TEST_USERNAME, TEST_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contents", is(updatedContent)));

        // 4. Verify update
        mockMvc.perform(get("/api/snippets/" + snippetId)
                        .with(httpBasic(TEST_USERNAME, TEST_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contents", is(updatedContent)));

        // 5. Delete
        mockMvc.perform(delete("/api/snippets/" + snippetId)
                        .with(httpBasic(TEST_USERNAME, TEST_PASSWORD)))
                .andExpect(status().isNoContent());

        // 6. Verify deletion
        mockMvc.perform(get("/api/snippets/" + snippetId)
                        .with(httpBasic(TEST_USERNAME, TEST_PASSWORD)))
                .andExpect(status().isNotFound());
    }

    @Test
    void multipleUsers_CannotAccessEachOthersSnippets() throws Exception {
        // testUser creates a snippet
        String testUserContent = "TestUser's private snippet";
        String createResponse = mockMvc.perform(post("/api/snippets")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("contents", testUserContent)
                        .with(httpBasic(TEST_USERNAME, TEST_PASSWORD)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Snippet testUserSnippet = objectMapper.readValue(createResponse, Snippet.class);

        // otherUser tries to access testUser's snippet
        mockMvc.perform(get("/api/snippets/" + testUserSnippet.getId())
                        .with(httpBasic(OTHER_USERNAME, OTHER_PASSWORD)))
                .andExpect(status().isNotFound());

        // otherUser tries to update testUser's snippet
        Map<String, String> updates = new HashMap<>();
        updates.put("contents", "Hacked!");

        mockMvc.perform(patch("/api/snippets/" + testUserSnippet.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updates))
                        .with(httpBasic(OTHER_USERNAME, OTHER_PASSWORD)))
                .andExpect(status().isForbidden());

        // otherUser tries to delete testUser's snippet
        mockMvc.perform(delete("/api/snippets/" + testUserSnippet.getId())
                        .with(httpBasic(OTHER_USERNAME, OTHER_PASSWORD)))
                .andExpect(status().isForbidden());

        // Verify testUser's snippet is unchanged
        mockMvc.perform(get("/api/snippets/" + testUserSnippet.getId())
                        .with(httpBasic(TEST_USERNAME, TEST_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contents", is(testUserContent)));
    }
}

