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

import java.util.List;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class JsnippetsApplicationTests {

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

        Date now = new Date();

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

	@Test
	void apiIntegrationTests() {
		// Authenticate as Alice
		TestRestTemplate aliceRest = restTemplate.withBasicAuth("alice", "password");
		// List Alice's snippets
		ResponseEntity<String> listResp = aliceRest.getForEntity("/api/snippets", String.class);
		assertEquals(HttpStatus.OK, listResp.getStatusCode());
        assertNotNull(listResp.getBody());
		assertTrue(listResp.getBody().contains("Lorem ipsum"));

		// View Alice's snippet
		List<Snippet> aliceSnippets = snippetRepository.findByPosterId(userRepository.findByUsername("alice").get().getId());
		Long snippetId = aliceSnippets.get(0).getId();
		ResponseEntity<Snippet> viewResp = aliceRest.getForEntity("/api/snippets/" + snippetId, Snippet.class);
		assertEquals(HttpStatus.OK, viewResp.getStatusCode());
        assertNotNull(viewResp.getBody());
		assertEquals("Lorem ipsum dolor sit amet.", viewResp.getBody().getContents());

		// Create a new snippet
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
		HttpEntity<String> createReq = new HttpEntity<>("contents=New snippet for Alice", headers);
		ResponseEntity<Snippet> createResp = aliceRest.postForEntity("/api/snippets", createReq, Snippet.class);
		assertEquals(HttpStatus.CREATED, createResp.getStatusCode());
        assertNotNull(createResp.getBody());
		assertEquals("New snippet for Alice", createResp.getBody().getContents());

		// Edit the new snippet
		Long newSnippetId = createResp.getBody().getId();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String jsonBody = "{\"contents\": \"Edited snippet for Alice\"}";
        HttpEntity<String> editReq = new HttpEntity<>(jsonBody, headers);
		Snippet editResp = aliceRest.patchForObject("/api/snippets/" + newSnippetId, editReq, Snippet.class);
        assertNotNull(editResp);
		assertEquals("Edited snippet for Alice", editResp.getContents());

		// Delete the new snippet
		ResponseEntity<Void> deleteResp = aliceRest.exchange("/api/snippets/" + newSnippetId, HttpMethod.DELETE, null, Void.class);
		assertEquals(HttpStatus.NO_CONTENT, deleteResp.getStatusCode());
		assertFalse(snippetRepository.findById(newSnippetId).isPresent());

        // Attempt to view Bob's snippet as Alice (should be forbidden)
        List<Snippet> bobSnippets = snippetRepository.findByPosterId(userRepository.findByUsername("bob").get().getId());
        Long bobSnippetId = bobSnippets.get(0).getId();
        ResponseEntity<Snippet> forbiddenResp = aliceRest.getForEntity("/api/snippets/" + bobSnippetId, Snippet.class);
        assertEquals(HttpStatus.NOT_FOUND, forbiddenResp.getStatusCode());
	}
}
