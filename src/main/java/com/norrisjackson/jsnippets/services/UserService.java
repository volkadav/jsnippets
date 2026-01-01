package com.norrisjackson.jsnippets.services;

import com.norrisjackson.jsnippets.data.User;
import com.norrisjackson.jsnippets.data.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@Transactional(readOnly = true)
public class UserService implements UserDetailsService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Check if a user with the given username exists.
     *
     * @param username the username to check
     * @return true if the username exists, false otherwise
     */
    public boolean userExists(String username) {
        return userRepository.existsByUsername(username);
    }

    /**
     * Check if a user with the given email exists.
     *
     * @param email the email address to check
     * @return true if the email exists, false otherwise
     */
    public boolean emailExists(String email) {
        return userRepository.existsByEmail(email);
    }

    /**
     * Authenticate a user with username and password.
     *
     * @param username the username
     * @param password the plain text password
     * @return true if authentication succeeds, false otherwise
     */
    public boolean authenticateUser(String username, String password) {
        Optional<User> user = userRepository.findByUsername(username);
        if (user.isEmpty()) {
            return false;
        }

        User u = user.get();
        return passwordEncoder.matches(password, u.getPasswordHash());
    }

    /**
     * Create a new user account.
     *
     * @param username the username for the new user
     * @param password the plain text password (will be hashed)
     * @param email    the email address
     * @return Optional containing the created User, or empty if creation fails
     */
    @Transactional
    public Optional<User> createUser(String username,
                           String password,
                           String email) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setCreatedAt(java.time.Instant.now());

        return Optional.of(userRepository.save(user));
    }

    /**
     * Get a user by username.
     *
     * @param username the username to search for
     * @return Optional containing the User if found, empty otherwise
     */
    public Optional<User> getUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    /**
     * Update an existing user.
     *
     * @param user the user entity to update
     * @return the updated user
     */
    @Transactional
    public User updateUser(User user) {
        return userRepository.save(user);
    }

    /**
     * Make one user follow another user.
     *
     * @param follower the user who will follow
     * @param toFollow the user to be followed
     * @return true if follow was successful, false if already following or attempting to follow self
     */
    @Transactional
    public boolean followUser(User follower, User toFollow) {
        if (follower.getId().equals(toFollow.getId())) {
            return false; // Cannot follow oneself
        }
        if (userRepository.isFollowing(follower.getId(), toFollow.getId())) {
            return false; // Already following
        }

        // Re-fetch the follower with followedUsers eagerly loaded to avoid LazyInitializationException
        User managedFollower = userRepository.findByIdWithFollowedUsers(follower.getId())
                .orElseThrow(() -> new IllegalStateException("Follower user not found"));

        managedFollower.getFollowedUsers().add(toFollow);

        return true;
    }

    /**
     * Make one user unfollow another user.
     *
     * @param follower the user who will unfollow
     * @param toUnfollow the user to be unfollowed
     * @return true if unfollow was successful, false if not currently following
     */
    @Transactional
    public boolean unfollowUser(User follower, User toUnfollow) {
        if (!userRepository.isFollowing(follower.getId(), toUnfollow.getId())) {
            return false; // Not following
        }

        // Re-fetch the follower with followedUsers eagerly loaded to avoid LazyInitializationException
        User managedFollower = userRepository.findByIdWithFollowedUsers(follower.getId())
                .orElseThrow(() -> new IllegalStateException("Follower user not found"));

        managedFollower.getFollowedUsers().remove(toUnfollow);

        return true;
    }

    /**
     * Check if one user is following another.
     *
     * @param follower the potential follower
     * @param followed the potentially followed user
     * @return true if follower is following followed, false otherwise
     */
    public boolean isFollowing(User follower, User followed) {
        if (follower == null || followed == null) {
            return false;
        }
        return userRepository.isFollowing(follower.getId(), followed.getId());
    }

    /**
     * Get the count of followers for a user.
     *
     * @param user the user to count followers for
     * @return the number of followers
     */
    public long getFollowerCount(User user) {
        if (user == null || user.getId() == null) {
            return 0;
        }
        return userRepository.countFollowersByUserId(user.getId());
    }

    /**
     * Get the count of users that a user is following.
     *
     * @param user the user to count following for
     * @return the number of users being followed
     */
    public long getFollowingCount(User user) {
        if (user == null || user.getId() == null) {
            return 0;
        }
        return userRepository.countFollowedUsersByUserId(user.getId());
    }

    /**
     * Get the list of user IDs that a user is following.
     *
     * @param userId the ID of the user
     * @return list of IDs of users being followed
     */
    public List<Long> getFollowedUserIds(Long userId) {
        if (userId == null) {
            return List.of();
        }
        return userRepository.findFollowedUsersByUserId(userId).stream()
                .map(User::getId)
                .toList();
    }

    /**
     * Get the list of users that a user is following.
     *
     * @param userId the ID of the user
     * @return list of users being followed
     */
    public List<User> getFollowedUsers(Long userId) {
        if (userId == null) {
            return List.of();
        }
        return userRepository.findFollowedUsersByUserId(userId);
    }

    @Override
    public UserDetails loadUserByUsername(String username) {
        Optional<User> user = userRepository.findByUsername(username);
        if (user.isEmpty()) {
            throw new UsernameNotFoundException("User not found ({})" + username);
        }
        User u = user.get();

        return org.springframework.security.core.userdetails.User
                .withUsername(u.getUsername())
                .password(u.getPasswordHash())
                .authorities("USER")
                .build();
    }
}
