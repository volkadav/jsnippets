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
}

