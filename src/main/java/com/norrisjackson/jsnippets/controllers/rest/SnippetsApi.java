package com.norrisjackson.jsnippets.controllers.rest;

import com.norrisjackson.jsnippets.configs.PaginationConfig;
import com.norrisjackson.jsnippets.data.Snippet;
import com.norrisjackson.jsnippets.data.User;
import com.norrisjackson.jsnippets.controllers.rest.dto.ApiError;
import com.norrisjackson.jsnippets.controllers.rest.dto.ErrorCodes;
import com.norrisjackson.jsnippets.controllers.rest.dto.PageResponse;
import com.norrisjackson.jsnippets.controllers.rest.dto.SnippetResponse;
import com.norrisjackson.jsnippets.services.SnippetService;
import com.norrisjackson.jsnippets.services.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
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

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping(path = "/api/v1/snippets", produces = "application/json")
@Slf4j
@Tag(name = "Snippets", description = "Operations related to JSnippets snippet data")
public class SnippetsApi {
    private final SnippetService snippetService;
    private final UserService userService;
    private final PaginationConfig paginationConfig;

    public SnippetsApi(SnippetService snippetService, UserService userService, PaginationConfig paginationConfig) {
        this.snippetService = snippetService;
        this.userService = userService;
        this.paginationConfig = paginationConfig;
    }

    private static class UserOrError {
        final User user;
        final ResponseEntity<?> errorResponse;
        UserOrError(User user) { this.user = user; this.errorResponse = null; }
        UserOrError(ResponseEntity<?> errorResponse) { this.user = null; this.errorResponse = errorResponse; }
        boolean hasError() { return errorResponse != null; }
    }

    private UserOrError getCurrentUserOrError(UserDetails authedUser, HttpServletRequest request) {
        if (authedUser == null) {
            log.error("No authenticated user present");
            return new UserOrError(ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(ApiError.of(ErrorCodes.AUTH_TOKEN_MISSING, "Authentication required", request.getRequestURI())));
        }
        Optional<User> userOpt = userService.getUserByUsername(authedUser.getUsername());
        if (userOpt.isEmpty()) {
            log.error("Authenticated user not found in database: {}", authedUser.getUsername());
            return new UserOrError(ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(ApiError.of(ErrorCodes.USER_NOT_FOUND, "User not found", request.getRequestURI())));
        }
        return new UserOrError(userOpt.get());
    }

    @GetMapping
    @Operation(summary = "Get paginated list of snippets for the authenticated user")
    public ResponseEntity<?> findAllForUser(
                                     @AuthenticationPrincipal UserDetails authedUser,
                                     @Parameter(description = "Page number") @RequestParam("pageNumber") Optional<Integer> pageNumber,
                                     @Parameter(description = "Page size") @RequestParam("pageSize") Optional<Integer> pageSize,
                                     HttpServletRequest request) {
        UserOrError userOrError = getCurrentUserOrError(authedUser, request);
        if (userOrError.hasError()) return userOrError.errorResponse;
        User currentUser = userOrError.user;

        int effectivePageSize = paginationConfig.getEffectivePageSize(pageSize.orElse(null));
        PageRequest page = PageRequest.of(pageNumber.orElse(0), effectivePageSize,
                Sort.by(Sort.Direction.DESC, "editedAt")
        );

        Page<Snippet> snippetPage = snippetService.getSnippetsByPosterId(currentUser.getId(), page);
        List<SnippetResponse> content = snippetPage.getContent().stream()
                .map(SnippetResponse::from)
                .toList();

        return ResponseEntity.ok(PageResponse.from(snippetPage, content));
    }

    @GetMapping("/{snippetId}")
    @Operation(summary = "Get a specific snippet by ID for the authenticated user")
    public ResponseEntity<?> findById(@AuthenticationPrincipal UserDetails authedUser,
                                      @Parameter(description = "Snippet ID") @PathVariable("snippetId") Long snippetId,
                                      HttpServletRequest request) {
        UserOrError userOrError = getCurrentUserOrError(authedUser, request);
        if (userOrError.hasError()) return userOrError.errorResponse;
        User currentUser = userOrError.user;

        Optional<Snippet> s = snippetService.retrieveSnippetForUser(snippetId, currentUser.getId());

        return s.<ResponseEntity<?>>map(snippet -> ResponseEntity.ok(SnippetResponse.from(snippet)))
                .orElseGet(() -> ResponseEntity
                        .status(HttpStatus.NOT_FOUND)
                        .body(ApiError.of(ErrorCodes.SNIPPET_NOT_FOUND, "Snippet not found", request.getRequestURI())));
    }

    @PostMapping(consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    @Operation(summary = "Create a new snippet for the authenticated user")
    public ResponseEntity<?> addSnippet(@AuthenticationPrincipal UserDetails authedUser,
                                        @Parameter(description = "Snippet text contents") @RequestParam String contents,
                                        HttpServletRequest request) {
        UserOrError userOrError = getCurrentUserOrError(authedUser, request);
        if (userOrError.hasError()) return userOrError.errorResponse;
        User currentUser = userOrError.user;

        if (Strings.isBlank(contents)) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(ApiError.of(ErrorCodes.VALIDATION_ERROR, "Snippet contents cannot be empty", request.getRequestURI()));
        }

        Snippet newSnippet = snippetService.createSnippet(contents, currentUser);
        if (newSnippet == null) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiError.of(ErrorCodes.INTERNAL_ERROR, "Failed to create snippet", request.getRequestURI()));
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(SnippetResponse.from(newSnippet));
    }

    @PatchMapping(path = "/{snippetId}", consumes = "application/json")
    @Operation(summary = "Edit an existing snippet by ID for the authenticated user")
    public ResponseEntity<?> editSnippet(
            @AuthenticationPrincipal UserDetails authedUser,
            @Parameter(description = "Snippet ID") @PathVariable("snippetId") Long snippetId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "JSON body with fields to update")
            @RequestBody Map<String, String> updates,
            HttpServletRequest request) {

        UserOrError userOrError = getCurrentUserOrError(authedUser, request);
        if (userOrError.hasError()) return userOrError.errorResponse;
        User currentUser = userOrError.user;

        String newContents = updates.get("contents");
        if (newContents == null || newContents.isBlank()) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(ApiError.of(ErrorCodes.VALIDATION_ERROR, "Snippet contents cannot be empty", request.getRequestURI()));
        }

        if (!snippetService.userOwnsSnippet(snippetId, currentUser.getId())) {
            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .body(ApiError.of(ErrorCodes.FORBIDDEN, "You do not have permission to edit this snippet", request.getRequestURI()));
        }

        Optional<Snippet> updated = snippetService.updateSnippet(snippetId, newContents, currentUser);
        return updated.<ResponseEntity<?>>map(snippet -> ResponseEntity.ok(SnippetResponse.from(snippet)))
                .orElseGet(() -> ResponseEntity
                        .status(HttpStatus.BAD_REQUEST)
                        .body(ApiError.of(ErrorCodes.INVALID_REQUEST, "Failed to update snippet", request.getRequestURI())));
    }

    @DeleteMapping("/{snippetId}")
    @Operation(summary = "Delete a snippet by ID for the authenticated user")
    public ResponseEntity<?> deleteSnippet(@AuthenticationPrincipal UserDetails authedUser,
                                           @Parameter(description = "Snippet ID") @PathVariable Long snippetId,
                                           HttpServletRequest request) {
        UserOrError userOrError = getCurrentUserOrError(authedUser, request);
        if (userOrError.hasError()) {
            return userOrError.errorResponse;
        }
        User currentUser = userOrError.user;
        if (!snippetService.userOwnsSnippet(snippetId, currentUser.getId())) {
            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .body(ApiError.of(ErrorCodes.FORBIDDEN, "You do not have permission to delete this snippet", request.getRequestURI()));
        }
        try {
            snippetService.deleteSnippet(snippetId, currentUser);
            return ResponseEntity.noContent().build(); // 204 No Content
        } catch (EmptyResultDataAccessException e) {
            log.warn("someone tried to delete a snippet with id {} that doesn't exist", snippetId);
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(ApiError.of(ErrorCodes.SNIPPET_NOT_FOUND, "Snippet not found", request.getRequestURI()));
        }
    }
}
