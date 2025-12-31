package com.norrisjackson.jsnippets.data;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.QueryHint;
import java.util.List;

/**
 * Repository for managing Snippet entities.
 * All queries use parameterized JPQL to prevent SQL injection.
 */
public interface SnippetRepository extends JpaRepository<Snippet, Long> {

    /**
     * Find all snippets by poster ID with eager fetching of the poster to prevent N+1 queries.
     * Uses parameterized query to prevent SQL injection.
     *
     * @param posterId the ID of the user who posted the snippets (must not be null)
     * @return list of snippets posted by the specified user
     */
    @Query("SELECT s FROM Snippet s JOIN FETCH s.poster WHERE s.poster.id = :posterId")
    @QueryHints(@QueryHint(name = "org.hibernate.cacheable", value = "true"))
    List<Snippet> findByPosterId(@Param("posterId") Long posterId);

    /**
     * Find all snippets by poster ID with sorting and eager fetching.
     * Uses parameterized query to prevent SQL injection.
     *
     * @param posterId the ID of the user who posted the snippets (must not be null)
     * @param sort the sort specification
     * @return sorted list of snippets posted by the specified user
     */
    @Query("SELECT s FROM Snippet s JOIN FETCH s.poster WHERE s.poster.id = :posterId")
    List<Snippet> findByPosterId(@Param("posterId") Long posterId, Sort sort);

    /**
     * Find all snippets by poster ID with pagination and eager fetching.
     * Uses parameterized query to prevent SQL injection.
     *
     * @param posterId the ID of the user who posted the snippets (must not be null)
     * @param pageable the pagination information
     * @return page of snippets posted by the specified user
     */
    @Query(value = "SELECT s FROM Snippet s JOIN FETCH s.poster WHERE s.poster.id = :posterId",
           countQuery = "SELECT COUNT(s) FROM Snippet s WHERE s.poster.id = :posterId")
    Page<Snippet> findByPosterId(@Param("posterId") Long posterId, Pageable pageable);

    /**
     * Count snippets by poster ID.
     * Uses parameterized query to prevent SQL injection.
     *
     * @param posterId the ID of the user who posted the snippets (must not be null)
     * @return count of snippets posted by the specified user
     */
    @Query("SELECT COUNT(s) FROM Snippet s WHERE s.poster.id = :posterId")
    long countByPosterId(@Param("posterId") Long posterId);

    /**
     * Find all snippets from a user and all users they follow (timeline view) with pagination.
     * Uses parameterized query to prevent SQL injection.
     *
     * @param userId the ID of the current user
     * @param followedUserIds the IDs of users the current user follows
     * @param pageable the pagination information
     * @return page of snippets from the user and followed users
     */
    @Query(value = "SELECT s FROM Snippet s JOIN FETCH s.poster WHERE s.poster.id = :userId OR s.poster.id IN :followedUserIds",
           countQuery = "SELECT COUNT(s) FROM Snippet s WHERE s.poster.id = :userId OR s.poster.id IN :followedUserIds")
    Page<Snippet> findTimelineSnippets(@Param("userId") Long userId,
                                       @Param("followedUserIds") List<Long> followedUserIds,
                                       Pageable pageable);

    /**
     * Find all snippets from a user only (timeline view when not following anyone) with pagination.
     * Uses parameterized query to prevent SQL injection.
     *
     * @param userId the ID of the current user
     * @param pageable the pagination information
     * @return page of snippets from the user only
     */
    @Query(value = "SELECT s FROM Snippet s JOIN FETCH s.poster WHERE s.poster.id = :userId",
           countQuery = "SELECT COUNT(s) FROM Snippet s WHERE s.poster.id = :userId")
    Page<Snippet> findTimelineSnippetsForUserOnly(@Param("userId") Long userId, Pageable pageable);

    /**
     * Find timeline snippets filtered by a specific poster with pagination.
     * Uses parameterized query to prevent SQL injection.
     *
     * @param userId the ID of the current user (for validation)
     * @param followedUserIds the IDs of users the current user follows
     * @param filterUserId the ID of the user to filter by
     * @param pageable the pagination information
     * @return page of snippets filtered by the specified user
     */
    @Query(value = "SELECT s FROM Snippet s JOIN FETCH s.poster WHERE s.poster.id = :filterUserId AND (s.poster.id = :userId OR s.poster.id IN :followedUserIds)",
           countQuery = "SELECT COUNT(s) FROM Snippet s WHERE s.poster.id = :filterUserId AND (s.poster.id = :userId OR s.poster.id IN :followedUserIds)")
    Page<Snippet> findTimelineSnippetsFilteredByUser(@Param("userId") Long userId,
                                                      @Param("followedUserIds") List<Long> followedUserIds,
                                                      @Param("filterUserId") Long filterUserId,
                                                      Pageable pageable);
}

