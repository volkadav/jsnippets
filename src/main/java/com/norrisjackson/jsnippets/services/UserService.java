package com.norrisjackson.jsnippets.services;

import com.norrisjackson.jsnippets.data.User;
import com.norrisjackson.jsnippets.data.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;

@Service
@Slf4j
public class UserService {
    private final UserRepository userRepository;
    private static final int ITERATIONS = 65536;
    private static final int KEY_LENGTH = 256;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public boolean userExists(String username) {
        return userRepository.findByName(username) != null;
    }

    public boolean emailExists(String email) {
        return userRepository.findByEmail(email) != null;
    }

    public boolean authenticateUser(String username, String password) {
        User user = userRepository.findByName(username);
        if (user == null) {
            return false;
        }

        byte[] salt = Base64.getDecoder().decode(user.getPasswordSalt());
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_LENGTH);
        try {
            SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            String hash = Base64.getEncoder().encodeToString(skf.generateSecret(spec).getEncoded());
            return hash.equals(user.getPasswordHash());
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            log.error("Error during authentication", e);
            return false;
        }
    }

    public User createUser(String username,
                           String password,
                           String email) {
        User user = new User();
        user.setName(username);
        user.setEmail(email);

        SecureRandom sr = new SecureRandom();
        byte[] salt = new byte[16];
        sr.nextBytes(salt);
        user.setPasswordSalt(Base64.getEncoder().encodeToString(salt));

        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_LENGTH);
        try {
            SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            user.setPasswordHash(Base64.getEncoder().encodeToString(skf.generateSecret(spec).getEncoded()));
        } catch (NoSuchAlgorithmException nsae) {
            log.error("No such algorithm for password hashing", nsae);
            return null;
        } catch (InvalidKeySpecException ikse) {
            log.error("Invalid key spec for password hashing", ikse);
            return null;
        }

        user.setCreatedAt(new java.util.Date());

        return userRepository.save(user);
    }

    public User getUserByUsername(String username) {
        return userRepository.findByName(username);
    }
}
