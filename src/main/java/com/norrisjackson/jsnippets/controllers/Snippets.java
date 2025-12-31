package com.norrisjackson.jsnippets.controllers;

import com.norrisjackson.jsnippets.configs.PaginationConfig;
import com.norrisjackson.jsnippets.data.Snippet;
import com.norrisjackson.jsnippets.data.User;
import com.norrisjackson.jsnippets.services.SnippetService;
import com.norrisjackson.jsnippets.services.UserService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
@Slf4j
public class Snippets {
    private final SnippetService snippetService;
    private final UserService userService;
    private final PaginationConfig paginationConfig;

    public Snippets(SnippetService snippetService, UserService userService, PaginationConfig paginationConfig) {
        this.snippetService = snippetService;
        this.userService = userService;
        this.paginationConfig = paginationConfig;
    }

    /**
     * Display a paginated list of snippets for a user.
     *
     * @param username optional username to view (defaults to current user)
     * @param page     optional page number
     * @param size     optional page size
     * @param sortDir  optional sort direction ("asc" or "desc")
     * @param model    the Spring MVC model
     * @return the snippet list view name
     */
    @PreAuthorize("isAuthenticated()")
    @GetMapping({"/snippets", "/snippets/{username}"})
    String snippets(@PathVariable(name = "username", required = false) String username,
                    @RequestParam(name = "page", required = false) Integer page,
                    @RequestParam(name = "size", required = false) Integer size,
                    @RequestParam(name = "sort", required = false) String sortDir,
                    Model model) {
        User currentUser = (User) model.getAttribute("currentUser");
        model.addAttribute("authUser", currentUser);
        User viewUser = currentUser;
        if (username != null && !username.equals(currentUser.getUsername())) {
            viewUser = userService.getUserByUsername(username).orElse(null);
            if (viewUser == null) {
                log.warn("User not found: {}", username);
                return "redirect:/snippets/" + currentUser.getUsername();
            }
        }
        model.addAttribute("viewUser", viewUser);

        Sort sort = Sort.by("editedAt");
        if (sortDir == null || sortDir.isBlank()) {
            sortDir = "desc";
        } else {
            sortDir = sortDir.toLowerCase();
        }
        model.addAttribute("sortDir", sortDir);
        if (sortDir.equals("asc")) {
            sort = sort.ascending();
        } else {
            sort = sort.descending();
        }

        // Pagination setup
        int pageNumber = (page == null || page < 0) ? 0 : page;
        int pageSize = paginationConfig.getEffectivePageSize(size);
        Pageable pageable = PageRequest.of(pageNumber, pageSize, sort);

        // Get paginated snippets
        Page<Snippet> snippetPage = snippetService.getSnippetsByPosterId(viewUser.getId(), pageable);
        long snippetCount = snippetPage.getTotalElements();
        log.info("Found {} snippets for user {} (page {} of {})",
                snippetCount, viewUser.getUsername(), pageNumber + 1, snippetPage.getTotalPages());

        model.addAttribute("snippets", snippetPage.getContent());
        model.addAttribute("snippetCount", snippetCount);
        model.addAttribute("page", snippetPage);
        model.addAttribute("currentPage", pageNumber);
        model.addAttribute("totalPages", snippetPage.getTotalPages());
        model.addAttribute("pageSize", pageSize);

        return "snippet/list";
    }

    /**
     * Display a consolidated timeline of snippets from the current user and users they follow.
     *
     * @param filterUser optional username to filter by
     * @param page       optional page number
     * @param size       optional page size
     * @param sortDir    optional sort direction ("asc" or "desc")
     * @param model      the Spring MVC model
     * @return the timeline view name
     */
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/timeline")
    String timeline(@RequestParam(name = "user", required = false) String filterUser,
                    @RequestParam(name = "page", required = false) Integer page,
                    @RequestParam(name = "size", required = false) Integer size,
                    @RequestParam(name = "sort", required = false) String sortDir,
                    Model model) {
        User currentUser = (User) model.getAttribute("currentUser");

        // Get list of followed users for filtering dropdown and query
        List<User> followedUsers = userService.getFollowedUsers(currentUser.getId());
        List<Long> followedUserIds = followedUsers.stream().map(User::getId).toList();

        model.addAttribute("followedUsers", followedUsers);
        model.addAttribute("followingCount", followedUsers.size());

        // Sort setup
        Sort sort = Sort.by("editedAt");
        if (sortDir == null || sortDir.isBlank()) {
            sortDir = "desc";
        } else {
            sortDir = sortDir.toLowerCase();
        }
        model.addAttribute("sortDir", sortDir);
        if (sortDir.equals("asc")) {
            sort = sort.ascending();
        } else {
            sort = sort.descending();
        }

        // Pagination setup
        int pageNumber = (page == null || page < 0) ? 0 : page;
        int pageSize = paginationConfig.getEffectivePageSize(size);
        Pageable pageable = PageRequest.of(pageNumber, pageSize, sort);

        // Get timeline snippets (optionally filtered by user)
        Page<Snippet> snippetPage;
        User filterUserEntity = null;

        if (filterUser != null && !filterUser.isBlank()) {
            // Filter by specific user
            filterUserEntity = userService.getUserByUsername(filterUser).orElse(null);
            if (filterUserEntity != null) {
                // Validate filter user is self or someone we follow
                if (filterUserEntity.getId().equals(currentUser.getId()) || followedUserIds.contains(filterUserEntity.getId())) {
                    snippetPage = snippetService.getTimelineSnippetsFilteredByUser(
                            currentUser.getId(), followedUserIds, filterUserEntity.getId(), pageable);
                } else {
                    // Invalid filter user, show all timeline
                    log.warn("User {} attempted to filter by user {} they don't follow", currentUser.getUsername(), filterUser);
                    snippetPage = snippetService.getTimelineSnippets(currentUser.getId(), followedUserIds, pageable);
                    filterUserEntity = null;
                }
            } else {
                // User not found, show all timeline
                log.warn("Filter user not found: {}", filterUser);
                snippetPage = snippetService.getTimelineSnippets(currentUser.getId(), followedUserIds, pageable);
            }
        } else {
            // No filter, show all timeline
            snippetPage = snippetService.getTimelineSnippets(currentUser.getId(), followedUserIds, pageable);
        }

        long snippetCount = snippetPage.getTotalElements();
        log.info("Found {} timeline snippets for user {} (page {} of {})",
                snippetCount, currentUser.getUsername(), pageNumber + 1, snippetPage.getTotalPages());

        model.addAttribute("snippets", snippetPage.getContent());
        model.addAttribute("snippetCount", snippetCount);
        model.addAttribute("page", snippetPage);
        model.addAttribute("currentPage", pageNumber);
        model.addAttribute("totalPages", snippetPage.getTotalPages());
        model.addAttribute("pageSize", pageSize);
        model.addAttribute("filterUser", filterUserEntity);

        return "snippet/timeline";
    }

    /**
     * Display the form for creating a new snippet.
     *
     * @param model the Spring MVC model
     * @return the new snippet form view name
     */
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/snippets/new")
    String newSnippet(Model model) {
        return "snippet/new";
    }

    /**
     * Handle new snippet form submission.
     *
     * @param contents the snippet contents
     * @param model the Spring MVC model
     * @return redirect URL
     */
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/snippets/new")
    String handleNewSnippet(@RequestParam String contents, Model model) {
        User currentUser = (User) model.getAttribute("currentUser");

        snippetService.createSnippet(contents, currentUser);

        return "redirect:/snippets";
    }

    /**
     * View a single snippet by ID.
     *
     * @param id the snippet ID
     * @param model the Spring MVC model
     * @return the snippet view name
     */
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/snippet/{id}")
    String viewSnippet(@PathVariable(name="id") Long id, Model model) {
        Snippet snippet = snippetService.getSnippetById(id).orElse(null);
        if (snippet == null) {
            log.warn("Snippet not found: id={}", id);
            return "redirect:/snippets";
        }

        model.addAttribute("snippet", snippet);

        User currentUser = (User) model.getAttribute("currentUser");
        model.addAttribute("isOwner",
                snippetService.userOwnsSnippet(snippet.getId(),
                currentUser.getId()));

        return "snippet/view";
    }

    /**
     * Display the form for editing a snippet.
     *
     * @param id the snippet ID
     * @param model the Spring MVC model
     * @return the edit snippet form view name
     */
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/snippet/{id}/edit")
    String editSnippet(@PathVariable(name="id") Long id, Model model) {
        Snippet snippet = snippetService.getSnippetById(id).orElse(null);
        if (snippet == null) {
            log.warn("Snippet not found: id={}", id);
            return "redirect:/snippets";
        }

        User currentUser = (User) model.getAttribute("currentUser");
        if (!snippetService.userOwnsSnippet(id, currentUser.getId())) {
            log.warn("User {} attempted to edit snippet {} they do not own", currentUser.getUsername(), id);
            return "redirect:/snippets";
        }

        model.addAttribute("snippet", snippet);
        return "snippet/edit";
    }

    /**
     * Handle snippet edit form submission.
     *
     * @param id the snippet ID
     * @param contents the updated snippet contents
     * @param model the Spring MVC model
     * @return redirect URL
     */
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/snippet/{id}/edit")
    String handleEditSnippet(@PathVariable(name="id") Long id, @RequestParam String contents, Model model) {
        User currentUser = (User) model.getAttribute("currentUser");
        if (!snippetService.userOwnsSnippet(id, currentUser.getId())) {
            log.warn("User {} attempted to edit snippet {} they do not own", currentUser.getUsername(), id);
            return "redirect:/snippets";
        }
        if (StringUtils.isBlank(contents)) {
            log.warn("Attempted to update snippet with empty contents");
            return "redirect:/snippet/" + id + "/edit";
        }

        snippetService.updateSnippet(id, contents, currentUser);
        return "redirect:/snippet/" + id;
    }

    /**
     * Display the snippet deletion confirmation page.
     *
     * @param id the snippet ID
     * @param model the Spring MVC model
     * @return the delete confirmation view name
     */
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/snippet/{id}/delete")
    String deleteSnippet(@PathVariable(name="id") Long id, Model model) {
        User currentUser = (User) model.getAttribute("currentUser");

        Snippet snippet = snippetService.getSnippetById(id).orElse(null);

        if (snippet == null) {
            log.warn("Snippet not found: id={}", id);
            return "redirect:/snippets";
        }

        if (!snippet.getPoster().getId().equals(currentUser.getId())) {
            log.warn("User {} attempted to delete snippet {} they do not own", currentUser.getUsername(), id);
            return "redirect:/snippets";
        }

        model.addAttribute("snippet", snippet);
        return "snippet/delete";
    }

    /**
     * Handle snippet deletion form submission.
     *
     * @param id the snippet ID
     * @param model the Spring MVC model
     * @return redirect URL
     */
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/snippet/{id}/delete")
    String handleDeleteSnippet(@PathVariable(name="id") Long id, Model model) {
        User currentUser = (User) model.getAttribute("currentUser");

        snippetService.deleteSnippet(id, currentUser);
        return "redirect:/snippets";
    }
}
