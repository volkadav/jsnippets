package com.norrisjackson.jsnippets.services;

import com.norrisjackson.jsnippets.data.User;
import com.norrisjackson.jsnippets.data.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private User testUser;
    private final String testUsername = "testuser";
    private final String testEmail = "test@example.com";
    private final String testPassword = "password123";
    private final String encodedPassword = "encodedPassword123";

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername(testUsername);
        testUser.setEmail(testEmail);
        testUser.setPasswordHash(encodedPassword);
        testUser.setCreatedAt(Instant.now());
        testUser.setTimezone("America/New_York");
    }

    @Test
    void userExists_WhenUserFound_ReturnsTrue() {
        // Given
        when(userRepository.existsByUsername(testUsername)).thenReturn(true);

        // When
        boolean exists = userService.userExists(testUsername);

        // Then
        assertThat(exists).isTrue();
        verify(userRepository).existsByUsername(testUsername);
    }

    @Test
    void userExists_WhenUserNotFound_ReturnsFalse() {
        // Given
        when(userRepository.existsByUsername(testUsername)).thenReturn(false);

        // When
        boolean exists = userService.userExists(testUsername);

        // Then
        assertThat(exists).isFalse();
        verify(userRepository).existsByUsername(testUsername);
    }

    @Test
    void emailExists_WhenEmailFound_ReturnsTrue() {
        // Given
        when(userRepository.existsByEmail(testEmail)).thenReturn(true);

        // When
        boolean exists = userService.emailExists(testEmail);

        // Then
        assertThat(exists).isTrue();
        verify(userRepository).existsByEmail(testEmail);
    }

    @Test
    void emailExists_WhenEmailNotFound_ReturnsFalse() {
        // Given
        when(userRepository.existsByEmail(testEmail)).thenReturn(false);

        // When
        boolean exists = userService.emailExists(testEmail);

        // Then
        assertThat(exists).isFalse();
        verify(userRepository).existsByEmail(testEmail);
    }

    @Test
    void authenticateUser_WhenUserExistsAndPasswordMatches_ReturnsTrue() {
        // Given
        when(userRepository.findByUsername(testUsername)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(testPassword, encodedPassword)).thenReturn(true);

        // When
        boolean authenticated = userService.authenticateUser(testUsername, testPassword);

        // Then
        assertThat(authenticated).isTrue();
        verify(userRepository).findByUsername(testUsername);
        verify(passwordEncoder).matches(testPassword, encodedPassword);
    }

    @Test
    void authenticateUser_WhenUserExistsButPasswordDoesNotMatch_ReturnsFalse() {
        // Given
        when(userRepository.findByUsername(testUsername)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(testPassword, encodedPassword)).thenReturn(false);

        // When
        boolean authenticated = userService.authenticateUser(testUsername, testPassword);

        // Then
        assertThat(authenticated).isFalse();
        verify(userRepository).findByUsername(testUsername);
        verify(passwordEncoder).matches(testPassword, encodedPassword);
    }

    @Test
    void authenticateUser_WhenUserDoesNotExist_ReturnsFalse() {
        // Given
        when(userRepository.findByUsername(testUsername)).thenReturn(Optional.empty());

        // When
        boolean authenticated = userService.authenticateUser(testUsername, testPassword);

        // Then
        assertThat(authenticated).isFalse();
        verify(userRepository).findByUsername(testUsername);
        verify(passwordEncoder, never()).matches(anyString(), anyString());
    }

    @Test
    void createUser_WhenValidData_ReturnsCreatedUser() {
        // Given
        User savedUser = new User();
        savedUser.setId(1L);
        savedUser.setUsername(testUsername);
        savedUser.setEmail(testEmail);
        savedUser.setPasswordHash(encodedPassword);
        savedUser.setCreatedAt(Instant.now());

        when(passwordEncoder.encode(testPassword)).thenReturn(encodedPassword);
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        // When
        Optional<User> result = userService.createUser(testUsername, testPassword, testEmail);

        // Then
        assertThat(result).isPresent();
        User createdUser = result.get();
        assertThat(createdUser.getUsername()).isEqualTo(testUsername);
        assertThat(createdUser.getEmail()).isEqualTo(testEmail);
        assertThat(createdUser.getPasswordHash()).isEqualTo(encodedPassword);
        assertThat(createdUser.getId()).isEqualTo(1L);

        verify(passwordEncoder).encode(testPassword);
        verify(userRepository).save(any(User.class));
    }

    @Test
    void getUserByUsername_WhenUserExists_ReturnsUser() {
        // Given
        when(userRepository.findByUsername(testUsername)).thenReturn(Optional.of(testUser));

        // When
        Optional<User> result = userService.getUserByUsername(testUsername);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(testUser);
        verify(userRepository).findByUsername(testUsername);
    }

    @Test
    void getUserByUsername_WhenUserDoesNotExist_ReturnsEmpty() {
        // Given
        when(userRepository.findByUsername(testUsername)).thenReturn(Optional.empty());

        // When
        Optional<User> result = userService.getUserByUsername(testUsername);

        // Then
        assertThat(result).isEmpty();
        verify(userRepository).findByUsername(testUsername);
    }

    @Test
    void loadUserByUsername_WhenUserExists_ReturnsUserDetails() {
        // Given
        when(userRepository.findByUsername(testUsername)).thenReturn(Optional.of(testUser));

        // When
        UserDetails userDetails = userService.loadUserByUsername(testUsername);

        // Then
        assertThat(userDetails).isNotNull();
        assertThat(userDetails.getUsername()).isEqualTo(testUsername);
        assertThat(userDetails.getPassword()).isEqualTo(encodedPassword);
        assertThat(userDetails.getAuthorities()).hasSize(1);
        assertThat(userDetails.getAuthorities().iterator().next().getAuthority()).isEqualTo("USER");
        verify(userRepository).findByUsername(testUsername);
    }

    @Test
    void loadUserByUsername_WhenUserDoesNotExist_ThrowsException() {
        // Given
        when(userRepository.findByUsername(testUsername)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> userService.loadUserByUsername(testUsername))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("User not found");
        verify(userRepository).findByUsername(testUsername);
    }

    @Test
    void updateUser_WhenValidUser_ReturnsUpdatedUser() {
        // Given
        testUser.setEmail("newemail@example.com");
        testUser.setTimezone("Europe/London");
        when(userRepository.save(testUser)).thenReturn(testUser);

        // When
        User result = userService.updateUser(testUser);

        // Then
        assertThat(result).isEqualTo(testUser);
        assertThat(result.getEmail()).isEqualTo("newemail@example.com");
        assertThat(result.getTimezone()).isEqualTo("Europe/London");
        verify(userRepository).save(testUser);
    }

    @Test
    void testFollowUser() {
        User follower = new User();
        follower.setId(1L);
        follower.setUsername("follower");

        User toFollow = new User();
        toFollow.setId(2L);
        toFollow.setUsername("toFollow");

        // Mock repository to return false for isFollowing (not yet following)
        when(userRepository.isFollowing(1L, 2L)).thenReturn(false);
        // Mock repository to return the follower with eagerly loaded followedUsers
        when(userRepository.findByIdWithFollowedUsers(1L)).thenReturn(Optional.of(follower));
        // Mock save to return the same user
        when(userRepository.save(follower)).thenReturn(follower);

        boolean result = userService.followUser(follower, toFollow);

        assertThat(result).isTrue();
        assertThat(follower.getFollowedUsers()).contains(toFollow);
        verify(userRepository).save(follower); // Verify save is called to persist changes
    }

    @Test
    void testFollowUserAlreadyFollowing() {
        User follower = new User();
        follower.setId(1L);
        follower.setUsername("follower");

        User toFollow = new User();
        toFollow.setId(2L);
        toFollow.setUsername("toFollow");

        follower.getFollowedUsers().add(toFollow);

        // Mock repository to return true for isFollowing check
        when(userRepository.isFollowing(1L, 2L)).thenReturn(true);

        boolean result = userService.followUser(follower, toFollow);

        assertThat(result).isFalse();
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void testUnfollowUser() {
        User follower = new User();
        follower.setId(1L);
        follower.setUsername("follower");

        User toUnfollow = new User();
        toUnfollow.setId(2L);
        toUnfollow.setUsername("toUnfollow");

        follower.getFollowedUsers().add(toUnfollow);

        // Mock repository to return true for isFollowing check
        when(userRepository.isFollowing(1L, 2L)).thenReturn(true);
        // Mock repository to return the follower with eagerly loaded followedUsers
        when(userRepository.findByIdWithFollowedUsers(1L)).thenReturn(Optional.of(follower));
        // Mock save to return the same user
        when(userRepository.save(follower)).thenReturn(follower);

        boolean result = userService.unfollowUser(follower, toUnfollow);

        assertThat(result).isTrue();
        assertThat(follower.getFollowedUsers()).doesNotContain(toUnfollow);
        verify(userRepository).save(follower); // Verify save is called to persist changes
    }

    @Test
    void testUnfollowUserNotFollowing() {
        User follower = new User();
        follower.setId(1L);
        follower.setUsername("follower");

        User toUnfollow = new User();
        toUnfollow.setId(2L);
        toUnfollow.setUsername("toUnfollow");

        boolean result = userService.unfollowUser(follower, toUnfollow);

        assertThat(result).isFalse();
        verify(userRepository, never()).save(any(User.class));
    }
}