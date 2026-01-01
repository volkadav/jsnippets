package com.norrisjackson.jsnippets.services;

import com.norrisjackson.jsnippets.data.Snippet;
import com.norrisjackson.jsnippets.data.SnippetRepository;
import com.norrisjackson.jsnippets.data.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@Transactional(readOnly = true)
public class SnippetService {
    private final SnippetRepository snippetRepository;

    public SnippetService(SnippetRepository snippetRepository) {
        this.snippetRepository = snippetRepository;
    }

    /**
     * Create a new snippet.
     *
     * @param snippetContent the content of the snippet
     * @param poster         the user who posted the snippet
     * @return the created Snippet object
     */
    @Transactional
    public Snippet createSnippet(String snippetContent, User poster) {
        Snippet snippet = new Snippet();
        snippet.setContents(snippetContent);
        snippet.setPoster(poster);

        Instant now = Instant.now();
        snippet.setCreatedAt(now);
        snippet.setEditedAt(now);

        return snippetRepository.save(snippet);
    }

    /**
     * Create a new snippet with a specific creation date.
     * Used for email-to-snippet feature where the email received date is used.
     *
     * @param snippetContent the snippet text content
     * @param poster         the user who owns the snippet
     * @param createdAt      the instant to use for createdAt and editedAt
     * @return the saved snippet
     */
    @Transactional
    public Snippet createSnippetWithDate(String snippetContent, User poster, Instant createdAt) {
        Snippet snippet = new Snippet();
        snippet.setContents(snippetContent);
        snippet.setPoster(poster);
        snippet.setCreatedAt(createdAt);
        snippet.setEditedAt(createdAt);
        return snippetRepository.save(snippet);
    }

    /**
     * Get a snippet by its ID.
     *
     * @param id the snippet ID
     * @return Optional containing the snippet if found, empty otherwise
     */
    public Optional<Snippet> getSnippetById(Long id) {
        return snippetRepository.findById(id);
    }

    /**
     * Get all snippets in the system.
     *
     * @return list of all snippets
     */
    public List<Snippet> getAllSnippets() {
        return snippetRepository.findAll();
    }

    /**
     * Get all snippets with pagination.
     *
     * @param pageable pagination parameters
     * @return page of snippets
     */
    public Page<Snippet> getAllSnippets(Pageable pageable) {
        return snippetRepository.findAll(pageable);
    }

    /**
     * Get all snippets for a specific user.
     *
     * @param posterId the user ID
     * @return list of snippets belonging to the user, empty list if posterId is invalid
     */
    public List<Snippet> getSnippetsByPosterId(Long posterId) {
        if (posterId == null || posterId <= 0) {
            log.warn("Invalid posterId provided: {}", posterId);
            return List.of();
        }
        return snippetRepository.findByPosterId(posterId);
    }

    /**
     * Count the number of snippets for a specific user.
     *
     * @param posterId the user ID
     * @return number of snippets belonging to the user, 0 if posterId is invalid
     */
    public long getSnippetCountByPosterId(Long posterId) {
        if (posterId == null || posterId <= 0) {
            log.warn("Invalid posterId provided for count: {}", posterId);
            return 0;
        }
        return snippetRepository.countByPosterId(posterId);
    }

    /**
     * Get all snippets for a specific user with sorting.
     *
     * @param posterId the user ID
     * @param sort sorting parameters
     * @return sorted list of snippets belonging to the user, empty list if posterId is invalid
     */
    public List<Snippet> getSnippetsByPosterId(Long posterId, Sort sort) {
        if (posterId == null || posterId <= 0) {
            log.warn("Invalid posterId provided: {}", posterId);
            return List.of();
        }
        return snippetRepository.findByPosterId(posterId, sort);
    }

    /**
     * Get all snippets for a specific user with pagination.
     *
     * @param posterId the user ID
     * @param pageable pagination parameters
     * @return page of snippets belonging to the user, empty page if posterId is invalid
     */
    public Page<Snippet> getSnippetsByPosterId(Long posterId, Pageable pageable) {
        if (posterId == null || posterId <= 0) {
            log.warn("Invalid posterId provided: {}", posterId);
            return Page.empty();
        }
        return snippetRepository.findByPosterId(posterId, pageable);
    }

    /**
     * Update an existing snippet's content.
     *
     * @param id the snippet ID
     * @param updatedSnippetContents the new content
     * @param editor the user attempting to update the snippet
     * @return Optional containing the updated snippet if successful, empty if validation fails
     */
    @Transactional
    public Optional<Snippet> updateSnippet(Long id, String updatedSnippetContents, User editor) {
        if (updatedSnippetContents == null || updatedSnippetContents.trim().isEmpty()) {
            log.warn("Attempted to update snippet with empty contents");
            return Optional.empty();
        }

        if (!userOwnsSnippet(id, editor.getId())) {
            log.warn("User {} attempted to update snippet {} they do not own", editor.getUsername(), id);
            return Optional.empty();
        }

        return snippetRepository.findById(id).map(existingSnippet -> {
            existingSnippet.setContents(updatedSnippetContents);

            Instant now = Instant.now();
            existingSnippet.setEditedAt(now);

            return snippetRepository.save(existingSnippet);
        });
    }

    /**
     * Delete a snippet.
     *
     * @param id the snippet ID
     * @param deleter the user attempting to delete the snippet
     * @return true if snippet was deleted, false if snippet doesn't exist or user doesn't own it
     */
    @Transactional
    public boolean deleteSnippet(Long id, User deleter) {
        if (snippetRepository.existsById(id) && userOwnsSnippet(id, deleter.getId())) {
            snippetRepository.deleteById(id);
            return true;
        }
        return false;
    }

    /**
     * Check if a user owns a specific snippet.
     *
     * @param snippetId the snippet ID
     * @param userId the user ID
     * @return true if the user owns the snippet, false otherwise
     */
    public boolean userOwnsSnippet(Long snippetId, Long userId) {
        Optional<Snippet> snippetOpt = snippetRepository.findById(snippetId);
        return snippetOpt.map(snippet -> snippet.getPoster().getId().equals(userId)).orElse(false);
    }

    /**
     * Retrieve a snippet if the user owns it.
     *
     * @param snippetId the snippet ID
     * @param userId the user ID
     * @return Optional containing the snippet if user owns it, empty otherwise
     */
    public Optional<Snippet> retrieveSnippetForUser(Long snippetId, Long userId) {
        Optional<Snippet> snippetOpt = snippetRepository.findById(snippetId);
        if (snippetOpt.isPresent() && userOwnsSnippet(snippetId, userId)) {
            return snippetOpt;
        }
        return Optional.empty();
    }

    /**
     * Get timeline snippets for a user - includes their own snippets and snippets from users they follow.
     *
     * @param userId the current user's ID
     * @param followedUserIds list of IDs of users the current user follows
     * @param pageable pagination parameters
     * @return page of timeline snippets
     */
    public Page<Snippet> getTimelineSnippets(Long userId, List<Long> followedUserIds, Pageable pageable) {
        if (userId == null || userId <= 0) {
            log.warn("Invalid userId provided for timeline: {}", userId);
            return Page.empty();
        }

        if (followedUserIds == null || followedUserIds.isEmpty()) {
            // User doesn't follow anyone, just return their own snippets
            return snippetRepository.findTimelineSnippetsForUserOnly(userId, pageable);
        }

        return snippetRepository.findTimelineSnippets(userId, followedUserIds, pageable);
    }

    /**
     * Get timeline snippets filtered by a specific user.
     *
     * @param userId the current user's ID
     * @param followedUserIds list of IDs of users the current user follows
     * @param filterUserId the user ID to filter by
     * @param pageable pagination parameters
     * @return page of timeline snippets filtered by the specified user
     */
    public Page<Snippet> getTimelineSnippetsFilteredByUser(Long userId, List<Long> followedUserIds,
                                                            Long filterUserId, Pageable pageable) {
        if (userId == null || userId <= 0) {
            log.warn("Invalid userId provided for timeline filter: {}", userId);
            return Page.empty();
        }

        if (filterUserId == null || filterUserId <= 0) {
            log.warn("Invalid filterUserId provided: {}", filterUserId);
            return Page.empty();
        }

        // If filtering by self and not following anyone, just return own snippets
        if (filterUserId.equals(userId) && (followedUserIds == null || followedUserIds.isEmpty())) {
            return snippetRepository.findTimelineSnippetsForUserOnly(userId, pageable);
        }

        // Validate that the filter user is either self or someone we follow
        if (!filterUserId.equals(userId) && (followedUserIds == null || !followedUserIds.contains(filterUserId))) {
            log.warn("User {} attempted to filter timeline by user {} they don't follow", userId, filterUserId);
            return Page.empty();
        }

        // If not following anyone, use the user-only query for filtering by self
        if (followedUserIds == null || followedUserIds.isEmpty()) {
            return snippetRepository.findTimelineSnippetsForUserOnly(filterUserId, pageable);
        }

        return snippetRepository.findTimelineSnippetsFilteredByUser(userId, followedUserIds, filterUserId, pageable);
    }
}
