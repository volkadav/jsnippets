package com.norrisjackson.jsnippets.services;

import com.norrisjackson.jsnippets.data.User;
import com.norrisjackson.jsnippets.data.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@Slf4j
public class UserService implements UserDetailsService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public boolean userExists(String username) {
        return userRepository.findByUsername(username).isPresent();
    }

    public boolean emailExists(String email) {
        return userRepository.findByEmail(email).isPresent();
    }

    public boolean authenticateUser(String username, String password) {
        Optional<User> user = userRepository.findByUsername(username);
        if (user.isEmpty()) {
            return false;
        }

        User u = user.get();
        return passwordEncoder.matches(password, u.getPasswordHash());
    }

    public Optional<User> createUser(String username,
                           String password,
                           String email) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setCreatedAt(new java.util.Date());

        return Optional.of(userRepository.save(user));
    }

    public Optional<User> getUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public User updateUser(User user) {
        return userRepository.save(user);
    }

    public boolean followUser(User follower, User toFollow) {
        if (follower.equals(toFollow)) {
            return false; // Cannot follow oneself
        }
        if (follower.getFollowedUsers().contains(toFollow)) {
            return false; // Already following
        }

        follower.getFollowedUsers().add(toFollow);
        userRepository.save(follower);

        return true;
    }

    public boolean unfollowUser(User follower, User toUnfollow) {
        if (!follower.getFollowedUsers().contains(toUnfollow)) {
            return false; // Not following
        }

        follower.getFollowedUsers().remove(toUnfollow);
        userRepository.save(follower);

        return true;
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
