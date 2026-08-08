package com.norrisjackson.jsnippets.controllers;

import com.norrisjackson.jsnippets.configs.PaginationConfig;
import com.norrisjackson.jsnippets.data.Snippet;
import com.norrisjackson.jsnippets.data.User;
import com.norrisjackson.jsnippets.services.SnippetService;
import com.norrisjackson.jsnippets.services.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@Slf4j
public class Search {
    private final SnippetService snippetService;
    private final UserService userService;
    private final PaginationConfig paginationConfig;

    public Search(SnippetService snippetService, UserService userService, PaginationConfig paginationConfig) {
        this.snippetService = snippetService;
        this.userService = userService;
        this.paginationConfig = paginationConfig;
    }

    /**
     * Search snippets and users by term, with pagination.
     * Both result sets are shown stacked on a single page.
     *
     * @param query optional search term
     * @param page  optional page number
     * @param size  optional page size
     * @param model the Spring MVC model
     * @return the search view name
     */
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/search")
    String search(@RequestParam(name = "q", required = false) String query,
                  @RequestParam(name = "page", required = false) Integer page,
                  @RequestParam(name = "size", required = false) Integer size,
                  Model model) {
        int pageNumber = (page == null || page < 0) ? 0 : page;
        int pageSize = paginationConfig.getEffectivePageSize(size);
        Pageable pageable = PageRequest.of(pageNumber, pageSize);

        String term = (query == null) ? "" : query.trim();
        model.addAttribute("query", term);

        if (term.isEmpty()) {
            model.addAttribute("snippets", java.util.List.of());
            model.addAttribute("snippetCount", 0L);
            model.addAttribute("users", java.util.List.of());
            model.addAttribute("userCount", 0L);
            model.addAttribute("page", Page.empty(pageable));
            model.addAttribute("currentPage", pageNumber);
            model.addAttribute("totalPages", 0);
            model.addAttribute("pageSize", pageSize);
            return "search";
        }

        Page<Snippet> snippetPage = snippetService.searchSnippets(term, pageable);
        Page<User> userPage = userService.searchUsers(term, pageable);
        log.info("Search '{}' returned {} snippets and {} users (page {} of {})",
                term, snippetPage.getTotalElements(), userPage.getTotalElements(),
                pageNumber + 1, snippetPage.getTotalPages());

        model.addAttribute("snippets", snippetPage.getContent());
        model.addAttribute("snippetCount", snippetPage.getTotalElements());
        model.addAttribute("users", userPage.getContent());
        model.addAttribute("userCount", userPage.getTotalElements());
        model.addAttribute("page", snippetPage);
        model.addAttribute("currentPage", pageNumber);
        model.addAttribute("totalPages", snippetPage.getTotalPages());
        model.addAttribute("pageSize", pageSize);

        return "search";
    }
}
