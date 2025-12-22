package com.norrisjackson.jsnippets.data;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.QueryHint;
import java.util.List;
import java.util.Optional;

/**
 * Repository for managing User entities.
 * All queries use parameterized JPQL to prevent SQL injection.
 */
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Find a user by username.
     * Uses parameterized query to prevent SQL injection.
     *
     * @param username the username to search for (must not be null)
     * @return Optional containing the user if found, empty otherwise
     */
    @Query("SELECT u FROM User u WHERE u.username = :username")
    @QueryHints(@QueryHint(name = "org.hibernate.cacheable", value = "true"))
    Optional<User> findByUsername(@Param("username") String username);

    /**
     * Find a user by email.
     * Uses parameterized query to prevent SQL injection.
     *
     * @param email the email to search for (must not be null)
     * @return Optional containing the user if found, empty otherwise
     */
    @Query("SELECT u FROM User u WHERE u.email = :email")
    @QueryHints(@QueryHint(name = "org.hibernate.cacheable", value = "true"))
    Optional<User> findByEmail(@Param("email") String email);

    /**
     * Find a user by username with followers eagerly loaded.
     * Uses parameterized query to prevent SQL injection.
     * Prevents N+1 queries by eagerly fetching followers.
     *
     * @param username the username to search for (must not be null)
     * @return Optional containing the user with followers if found, empty otherwise
     */
    @Query("SELECT u FROM User u LEFT JOIN FETCH u.followers WHERE u.username = :username")
    Optional<User> findByUsernameWithFollowers(@Param("username") String username);

    /**
     * Find a user by username with followed users eagerly loaded.
     * Uses parameterized query to prevent SQL injection.
     * Prevents N+1 queries by eagerly fetching followed users.
     *
     * @param username the username to search for (must not be null)
     * @return Optional containing the user with followed users if found, empty otherwise
     */
    @Query("SELECT u FROM User u LEFT JOIN FETCH u.followedUsers WHERE u.username = :username")
    Optional<User> findByUsernameWithFollowedUsers(@Param("username") String username);

    /**
     * Find a user by ID with followers eagerly loaded.
     * Uses parameterized query to prevent SQL injection.
     * Prevents N+1 queries by eagerly fetching followers.
     *
     * @param userId the ID of the user to search for (must not be null)
     * @return Optional containing the user with followers if found, empty otherwise
     */
    @Query("SELECT u FROM User u LEFT JOIN FETCH u.followers WHERE u.id = :userId")
    Optional<User> findByIdWithFollowers(@Param("userId") Long userId);

    /**
     * Find a user by ID with followed users eagerly loaded.
     * Uses parameterized query to prevent SQL injection.
     * Prevents N+1 queries by eagerly fetching followed users.
     *
     * @param userId the ID of the user to search for (must not be null)
     * @return Optional containing the user with followed users if found, empty otherwise
     */
    @Query("SELECT u FROM User u LEFT JOIN FETCH u.followedUsers WHERE u.id = :userId")
    Optional<User> findByIdWithFollowedUsers(@Param("userId") Long userId);

    /**
     * Count followers for a specific user.
     * Uses parameterized query to prevent SQL injection.
     *
     * @param userId the ID of the user (must not be null)
     * @return count of followers for the specified user
     */
    @Query("SELECT COUNT(f) FROM User u JOIN u.followers f WHERE u.id = :userId")
    long countFollowersByUserId(@Param("userId") Long userId);

    /**
     * Count followed users for a specific user.
     * Uses parameterized query to prevent SQL injection.
     *
     * @param userId the ID of the user (must not be null)
     * @return count of users followed by the specified user
     */
    @Query("SELECT COUNT(f) FROM User u JOIN u.followedUsers f WHERE u.id = :userId")
    long countFollowedUsersByUserId(@Param("userId") Long userId);

    /**
     * Check if a user exists by username.
     * Uses parameterized query to prevent SQL injection.
     * More efficient than findByUsername().isPresent() as it only returns a boolean.
     *
     * @param username the username to check (must not be null)
     * @return true if user exists, false otherwise
     */
    @Query("SELECT CASE WHEN COUNT(u) > 0 THEN true ELSE false END FROM User u WHERE u.username = :username")
    boolean existsByUsername(@Param("username") String username);

    /**
     * Check if a user exists by email.
     * Uses parameterized query to prevent SQL injection.
     * More efficient than findByEmail().isPresent() as it only returns a boolean.
     *
     * @param email the email to check (must not be null)
     * @return true if user exists, false otherwise
     */
    @Query("SELECT CASE WHEN COUNT(u) > 0 THEN true ELSE false END FROM User u WHERE u.email = :email")
    boolean existsByEmail(@Param("email") String email);

    /**
     * Find all followers of a user.
     * Uses parameterized query to prevent SQL injection.
     *
     * @param userId the ID of the user whose followers to retrieve (must not be null)
     * @return list of users who follow the specified user
     */
    @Query("SELECT f FROM User u JOIN u.followers f WHERE u.id = :userId")
    List<User> findFollowersByUserId(@Param("userId") Long userId);

    /**
     * Find all users followed by a user.
     * Uses parameterized query to prevent SQL injection.
     *
     * @param userId the ID of the user whose followed users to retrieve (must not be null)
     * @return list of users followed by the specified user
     */
    @Query("SELECT f FROM User u JOIN u.followedUsers f WHERE u.id = :userId")
    List<User> findFollowedUsersByUserId(@Param("userId") Long userId);
}
