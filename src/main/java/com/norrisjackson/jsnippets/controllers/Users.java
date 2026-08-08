package com.norrisjackson.jsnippets.controllers;

import com.norrisjackson.jsnippets.configs.PaginationConfig;
import com.norrisjackson.jsnippets.data.User;
import com.norrisjackson.jsnippets.services.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

@Controller
@Slf4j
public class Users {
    private final UserService userService;
    private final PaginationConfig paginationConfig;

    public Users(UserService userService, PaginationConfig paginationConfig) {
        this.userService = userService;
        this.paginationConfig = paginationConfig;
    }

    /**
     * Browse/search the user directory, with pagination.
     * Supports filtering by username or bio via the {@code q} parameter.
     *
     * @param query optional username/bio search term
     * @param page  optional page number
     * @param size  optional page size
     * @param model the Spring MVC model
     * @return the users directory view name
     */
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/users")
    String users(@RequestParam(name = "q", required = false) String query,
                 @RequestParam(name = "page", required = false) Integer page,
                 @RequestParam(name = "size", required = false) Integer size,
                 Model model) {
        User currentUser = (User) model.getAttribute("currentUser");

        int pageNumber = (page == null || page < 0) ? 0 : page;
        int pageSize = paginationConfig.getEffectivePageSize(size);
        Pageable pageable = PageRequest.of(pageNumber, pageSize, Sort.by("username"));

        String term = (query == null) ? "" : query.trim();
        model.addAttribute("query", term);

        Page<User> userPage = term.isEmpty()
                ? userService.getAllUsers(pageable)
                : userService.searchUsers(term, pageable);

        List<Long> userIds = userPage.getContent().stream().map(User::getId).toList();
        Map<Long, Long> followerCounts = userService.getFollowerCounts(userIds);
        Map<Long, Long> followingCounts = userService.getFollowingCounts(userIds);

        log.info("User directory query '{}' returned {} users (page {} of {})",
                term, userPage.getTotalElements(), pageNumber + 1, userPage.getTotalPages());

        model.addAttribute("users", userPage.getContent());
        model.addAttribute("followerCounts", followerCounts);
        model.addAttribute("followingCounts", followingCounts);
        model.addAttribute("currentUserId", currentUser != null ? currentUser.getId() : null);
        model.addAttribute("userCount", userPage.getTotalElements());
        model.addAttribute("page", userPage);
        model.addAttribute("currentPage", pageNumber);
        model.addAttribute("totalPages", userPage.getTotalPages());
        model.addAttribute("pageSize", pageSize);

        return "users";
    }
}
