package com.norrisjackson.jsnippets.controllers.rest;

import com.norrisjackson.jsnippets.data.Snippet;
import com.norrisjackson.jsnippets.data.User;
import com.norrisjackson.jsnippets.services.SnippetService;
import com.norrisjackson.jsnippets.services.UserService;
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
    public ResponseEntity<Page<Snippet>> findAllForUser(
                                     @AuthenticationPrincipal UserDetails authedUser,
                                     @RequestParam("pageNumber") Optional<Integer> pageNumber,
                                     @RequestParam("pageSize") Optional<Integer> pageSize) {
        UserOrError userOrError = getCurrentUserOrError(authedUser);
        if (userOrError.hasError()) return (ResponseEntity<Page<Snippet>>) userOrError.errorResponse;
        User currentUser = userOrError.user;

        PageRequest page = PageRequest.of(pageNumber.orElse(0), pageSize.orElse(20),
                Sort.by(Sort.Direction.DESC, "editedAt")
        );

        return ResponseEntity.ok(snippetService.getSnippetsByPosterId(currentUser.getId(), page));
    }

    @GetMapping("/{snippetId}")
    public ResponseEntity<Snippet> findById(@AuthenticationPrincipal UserDetails authedUser,
                                            @PathVariable("snippetId") Long snippetId) {
        UserOrError userOrError = getCurrentUserOrError(authedUser);
        if (userOrError.hasError()) return (ResponseEntity<Snippet>) userOrError.errorResponse;
        User currentUser = userOrError.user;

        Optional<Snippet> s = snippetService.retrieveSnippetForUser(snippetId, currentUser.getId());

        return s.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).body(null));
    }

    @PostMapping(consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<Snippet> addSnippet(@AuthenticationPrincipal UserDetails authedUser,
                                              @RequestParam String contents) {
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
    public ResponseEntity<Snippet> editSnippet(@AuthenticationPrincipal UserDetails authedUser,
                                               @PathVariable("snippetId") Long snippetId,
                                               @RequestBody Snippet snippetNew) {

        UserOrError userOrError = getCurrentUserOrError(authedUser);
        if (userOrError.hasError()) return (ResponseEntity<Snippet>) userOrError.errorResponse;
        User currentUser = userOrError.user;

        if (Strings.isBlank(snippetNew.getContents())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }

        if (!snippetService.userOwnsSnippet(snippetId, currentUser.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);
        }

        Optional<Snippet> updated = snippetService.updateSnippet(snippetId, snippetNew.getContents(), currentUser);
        return updated.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null));
    }

    @DeleteMapping("/{snippetId}")
    public ResponseEntity<Void> deleteSnippet(@AuthenticationPrincipal UserDetails authedUser,
                                              @PathVariable Long snippetId) {
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
