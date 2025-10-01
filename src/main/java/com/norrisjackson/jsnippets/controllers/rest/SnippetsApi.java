package com.norrisjackson.jsnippets.controllers.rest;

import com.norrisjackson.jsnippets.data.Snippet;
import com.norrisjackson.jsnippets.data.User;
import com.norrisjackson.jsnippets.services.SnippetService;
import com.norrisjackson.jsnippets.services.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping(path = "/api/snippets", produces = "application/json")
@Slf4j
@Tag(name = "Snippets", description = "Operations related to JSnippets snippet data")
public class SnippetsApi {
    private final SnippetService snippetService;
    private final UserService userService;

    public SnippetsApi(SnippetService snippetService, UserService userService) {
        this.snippetService = snippetService;
        this.userService = userService;
    }

    private static class UserOrError {
        final User user;
        final ResponseEntity<?> errorResponse;
        UserOrError(User user) { this.user = user; this.errorResponse = null; }
        UserOrError(ResponseEntity<?> errorResponse) { this.user = null; this.errorResponse = errorResponse; }
        boolean hasError() { return errorResponse != null; }
    }

    private UserOrError getCurrentUserOrError(UserDetails authedUser) {
        if (authedUser == null) {
            log.error("No authenticated user present");
            return new UserOrError(ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null));
        }
        Optional<User> userOpt = userService.getUserByUsername(authedUser.getUsername());
        if (userOpt.isEmpty()) {
            log.error("Authenticated user not found in database: {}", authedUser.getUsername());
            return new UserOrError(ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null));
        }
        return new UserOrError(userOpt.get());
    }

    @GetMapping
    @Operation(summary = "Get paginated list of snippets for the authenticated user")
    public ResponseEntity<Page<Snippet>> findAllForUser(
                                     @AuthenticationPrincipal UserDetails authedUser,
                                     @Parameter(description = "Page number") @RequestParam("pageNumber") Optional<Integer> pageNumber,
                                     @Parameter(description = "Page size") @RequestParam("pageSize") Optional<Integer> pageSize) {
        UserOrError userOrError = getCurrentUserOrError(authedUser);
        if (userOrError.hasError()) return (ResponseEntity<Page<Snippet>>) userOrError.errorResponse;
        User currentUser = userOrError.user;

        PageRequest page = PageRequest.of(pageNumber.orElse(0), pageSize.orElse(20),
                Sort.by(Sort.Direction.DESC, "editedAt")
        );

        return ResponseEntity.ok(snippetService.getSnippetsByPosterId(currentUser.getId(), page));
    }

    @GetMapping("/{snippetId}")
    @Operation(summary = "Get a specific snippet by ID for the authenticated user")
    public ResponseEntity<Snippet> findById(@AuthenticationPrincipal UserDetails authedUser,
                                            @Parameter(description = "Snippet ID") @PathVariable("snippetId") Long snippetId) {
        UserOrError userOrError = getCurrentUserOrError(authedUser);
        if (userOrError.hasError()) return (ResponseEntity<Snippet>) userOrError.errorResponse;
        User currentUser = userOrError.user;

        Optional<Snippet> s = snippetService.retrieveSnippetForUser(snippetId, currentUser.getId());

        return s.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).body(null));
    }

    @PostMapping(consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    @Operation(summary = "Create a new snippet for the authenticated user")
    public ResponseEntity<Snippet> addSnippet(@AuthenticationPrincipal UserDetails authedUser,
                                              @Parameter(description = "Snippet text contents") @RequestParam String contents) {
        UserOrError userOrError = getCurrentUserOrError(authedUser);
        if (userOrError.hasError()) return (ResponseEntity<Snippet>) userOrError.errorResponse;
        User currentUser = userOrError.user;

        if (Strings.isBlank(contents)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }

        Snippet newSnippet = snippetService.createSnippet(contents, currentUser);
        return (newSnippet == null) ?
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null) :
                ResponseEntity.status(HttpStatus.CREATED).body(newSnippet);
    }

    @PatchMapping(path = "/{snippetId}", consumes = "application/json")
    @Operation(summary = "Edit an existing snippet by ID for the authenticated user")
    public ResponseEntity<Snippet> editSnippet(@AuthenticationPrincipal UserDetails authedUser,
                                               @Parameter(description = "Snippet ID") @PathVariable("snippetId") Long snippetId,
                                               @Parameter(description = "New snippet text content") @RequestParam String newContents) {

        UserOrError userOrError = getCurrentUserOrError(authedUser);
        if (userOrError.hasError()) return (ResponseEntity<Snippet>) userOrError.errorResponse;
        User currentUser = userOrError.user;

        if (Strings.isBlank(newContents)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }

        if (!snippetService.userOwnsSnippet(snippetId, currentUser.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);
        }

        Optional<Snippet> updated = snippetService.updateSnippet(snippetId, newContents, currentUser);
        return updated.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null));
    }

    @DeleteMapping("/{snippetId}")
    @Operation(summary = "Delete a snippet by ID for the authenticated user")
    public ResponseEntity<Void> deleteSnippet(@AuthenticationPrincipal UserDetails authedUser,
                                              @Parameter(description = "Snippet ID") @PathVariable Long snippetId) {
        UserOrError userOrError = getCurrentUserOrError(authedUser);
        if (userOrError.hasError()) {
            return (ResponseEntity<Void>) userOrError.errorResponse;
        }
        User currentUser = userOrError.user;
        if (!snippetService.userOwnsSnippet(snippetId, currentUser.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        try {
            snippetService.deleteSnippet(snippetId, currentUser);
            return ResponseEntity.noContent().build(); // 204 No Content
        } catch (EmptyResultDataAccessException e) {
            log.info("someone tried to delete a snippet with id {} that doesn't exist", snippetId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }
}
