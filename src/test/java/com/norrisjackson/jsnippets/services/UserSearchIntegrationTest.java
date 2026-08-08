package com.norrisjackson.jsnippets.services;

import com.norrisjackson.jsnippets.data.User;
import com.norrisjackson.jsnippets.data.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class UserSearchIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

    @Test
    void bioSearch_isCaseInsensitive() {
        User u = new User();
        u.setUsername("dadcase");
        u.setEmail("dadcase@test.com");
        u.setPasswordHash("x");
        u.setBio("Dad jokes enthusiast");
        u.setCreatedAt(Instant.now());
        u.setTimezone("UTC");
        userRepository.save(u);

        PageRequest pageable = PageRequest.of(0, 20);

        Page<User> byLower = userService.searchUsers("dad", pageable);
        Page<User> byUpper = userService.searchUsers("DAD", pageable);
        Page<User> byMixed = userService.searchUsers("Dad", pageable);

        assertThat(byLower.getContent())
                .as("lowercase query matches mixed-case bio")
                .extracting(User::getUsername)
                .contains("dadcase");
        assertThat(byUpper.getContent())
                .as("uppercase query matches mixed-case bio")
                .extracting(User::getUsername)
                .contains("dadcase");
        assertThat(byMixed.getContent())
                .as("mixed-case query matches mixed-case bio")
                .extracting(User::getUsername)
                .contains("dadcase");
    }

    @Test
    void usernameSearch_isCaseInsensitive() {
        PageRequest pageable = PageRequest.of(0, 20);

        Page<User> byLower = userService.searchUsers("alice", pageable);
        Page<User> byUpper = userService.searchUsers("ALICE", pageable);

        assertThat(byLower.getContent()).extracting(User::getUsername).contains("alice");
        assertThat(byUpper.getContent()).extracting(User::getUsername).contains("alice");
    }
}
