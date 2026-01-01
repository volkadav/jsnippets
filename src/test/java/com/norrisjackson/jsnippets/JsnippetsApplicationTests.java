package com.norrisjackson.jsnippets;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import com.norrisjackson.jsnippets.data.Snippet;
import com.norrisjackson.jsnippets.data.User;
import com.norrisjackson.jsnippets.data.SnippetRepository;
import com.norrisjackson.jsnippets.data.UserRepository;
import com.norrisjackson.jsnippets.controllers.rest.dto.AuthenticationRequest;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class JsnippetsApplicationTests {

	// API paths (versioned)
	private static final String AUTH_LOGIN_PATH = "/api/v1/auth/login";
	private static final String SNIPPETS_PATH = "/api/v1/snippets";

	@Autowired
	private TestRestTemplate restTemplate;

	@Autowired
	private UserRepository userRepository;

    @Autowired
	private SnippetRepository snippetRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

	@BeforeAll
	static void setup(@Autowired UserRepository userRepository, @Autowired SnippetRepository snippetRepository, @Autowired PasswordEncoder passwordEncoder) {
		// Clear and populate test data
		snippetRepository.deleteAll();
		userRepository.deleteAll();

        Instant now = Instant.now();

		User alice = new User();
		alice.setUsername("alice");
		alice.setEmail("alice@example.com");
		alice.setPasswordHash(passwordEncoder.encode("password"));
        alice.setCreatedAt(now);
		userRepository.save(alice);

        User bob = new User();
		bob.setUsername("bob");
		bob.setEmail("bob@example.com");
		bob.setPasswordHash(passwordEncoder.encode("password"));
        bob.setCreatedAt(now);
		userRepository.save(bob);

        Snippet s1 = new Snippet();
		s1.setContents("Lorem ipsum dolor sit amet.");
		s1.setPoster(alice);
        s1.setCreatedAt(now);
        s1.setEditedAt(now);
		snippetRepository.save(s1);

        Snippet s2 = new Snippet();
		s2.setContents("Consectetur adipiscing elit.");
		s2.setPoster(bob);
        s2.setCreatedAt(now);
        s2.setEditedAt(now);
		snippetRepository.save(s2);
	}

	@Test
	void contextLoads() {
	}

	/**
	 * Helper method to get JWT token for a user
	 */
	private String getJwtToken(String username, String password) {
		AuthenticationRequest request = new AuthenticationRequest(username, password);
		ResponseEntity<Map> response = restTemplate.postForEntity(AUTH_LOGIN_PATH, request, Map.class);
		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertNotNull(response.getBody());
		return (String) response.getBody().get("token");
	}

	/**
	 * Helper method to create headers with JWT token
	 */
	private HttpHeaders createHeadersWithJwt(String token) {
		HttpHeaders headers = new HttpHeaders();
		headers.set("Authorization", "Bearer " + token);
		return headers;
	}

	@Test
	void apiIntegrationTests() {
		// Authenticate as Alice and get JWT token
		String aliceToken = getJwtToken("alice", "password");
		HttpHeaders aliceHeaders = createHeadersWithJwt(aliceToken);

		// List Alice's snippets
		HttpEntity<String> requestEntity = new HttpEntity<>(aliceHeaders);
		ResponseEntity<String> listResp = restTemplate.exchange(SNIPPETS_PATH, HttpMethod.GET, requestEntity, String.class);
		assertEquals(HttpStatus.OK, listResp.getStatusCode());
        assertNotNull(listResp.getBody());
		assertTrue(listResp.getBody().contains("Lorem ipsum"));

		// View Alice's snippet
		List<Snippet> aliceSnippets = snippetRepository.findByPosterId(userRepository.findByUsername("alice").get().getId());
		Long snippetId = aliceSnippets.get(0).getId();
		ResponseEntity<Snippet> viewResp = restTemplate.exchange(SNIPPETS_PATH + "/" + snippetId, HttpMethod.GET,
			new HttpEntity<>(aliceHeaders), Snippet.class);
		assertEquals(HttpStatus.OK, viewResp.getStatusCode());
        assertNotNull(viewResp.getBody());
		assertEquals("Lorem ipsum dolor sit amet.", viewResp.getBody().getContents());

		// Create a new snippet
		HttpHeaders createHeaders = createHeadersWithJwt(aliceToken);
		createHeaders.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
		HttpEntity<String> createReq = new HttpEntity<>("contents=New snippet for Alice", createHeaders);
		ResponseEntity<Snippet> createResp = restTemplate.postForEntity(SNIPPETS_PATH, createReq, Snippet.class);
		assertEquals(HttpStatus.CREATED, createResp.getStatusCode());
        assertNotNull(createResp.getBody());
		assertEquals("New snippet for Alice", createResp.getBody().getContents());

		// Edit the new snippet
		Long newSnippetId = createResp.getBody().getId();
		HttpHeaders editHeaders = createHeadersWithJwt(aliceToken);
        editHeaders.setContentType(MediaType.APPLICATION_JSON);
        String jsonBody = "{\"contents\": \"Edited snippet for Alice\"}";
        HttpEntity<String> editReq = new HttpEntity<>(jsonBody, editHeaders);
		ResponseEntity<Snippet> editResp = restTemplate.exchange(SNIPPETS_PATH + "/" + newSnippetId, HttpMethod.PATCH, editReq, Snippet.class);
        assertNotNull(editResp.getBody());
		assertEquals("Edited snippet for Alice", editResp.getBody().getContents());

		// Delete the new snippet
		ResponseEntity<Void> deleteResp = restTemplate.exchange(SNIPPETS_PATH + "/" + newSnippetId, HttpMethod.DELETE,
			new HttpEntity<>(aliceHeaders), Void.class);
		assertEquals(HttpStatus.NO_CONTENT, deleteResp.getStatusCode());
		assertFalse(snippetRepository.findById(newSnippetId).isPresent());

        // Attempt to view Bob's snippet as Alice (should be forbidden)
        List<Snippet> bobSnippets = snippetRepository.findByPosterId(userRepository.findByUsername("bob").get().getId());
        Long bobSnippetId = bobSnippets.get(0).getId();
        ResponseEntity<Snippet> forbiddenResp = restTemplate.exchange(SNIPPETS_PATH + "/" + bobSnippetId, HttpMethod.GET,
			new HttpEntity<>(aliceHeaders), Snippet.class);
        assertEquals(HttpStatus.NOT_FOUND, forbiddenResp.getStatusCode());
	}
}
