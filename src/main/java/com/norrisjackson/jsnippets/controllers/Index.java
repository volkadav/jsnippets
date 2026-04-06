package com.norrisjackson.jsnippets.controllers;

import com.norrisjackson.jsnippets.configs.PaginationConfig;
import com.norrisjackson.jsnippets.data.Snippet;
import com.norrisjackson.jsnippets.data.User;
import com.norrisjackson.jsnippets.services.SnippetService;
import com.norrisjackson.jsnippets.services.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Optional;

@Controller
@Slf4j
public class Index {
    private final UserService userService;
    private final SnippetService snippetService;
    private final PaginationConfig paginationConfig;

    public Index(UserService userService, SnippetService snippetService, PaginationConfig paginationConfig) {
        this.userService = userService;
        this.snippetService = snippetService;
        this.paginationConfig = paginationConfig;
    }

    /**
     * Display the main index/home page.
     * Shows paginated snippets for authenticated users.
     *
     * @param page  optional page number
     * @param size  optional page size
     * @param model the Spring MVC model
     * @return the view name
     */
    @GetMapping({"/", "/index"})
    public String index(@RequestParam(required = false) Integer page,
                        @RequestParam(required = false) Integer size,
                        Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Object principal = auth.getPrincipal();
        log.info("Principal: {}", principal);

        if (principal instanceof UserDetails authedUser) {
            Optional<User> userOpt = userService.getUserByUsername(
                    authedUser.getUsername());
            if (userOpt.isEmpty()) {
                log.error("Authenticated user not found in database: {}",
                        authedUser.getUsername());
                return "redirect:/logout";
            }
            User user = userOpt.get();

            model.addAttribute("greeting", "Welcome, " +
                    user.getUsername() + "!");
            model.addAttribute("username", user.getUsername());
            model.addAttribute("snippetCount",
                    snippetService.getSnippetCountByPosterId(user.getId()));

            if (page == null || page < 0) page = 0;
            int effectiveSize = paginationConfig.getEffectivePageSize(size);
            model.addAttribute("currentPage", page);
            model.addAttribute("pageSize", effectiveSize);

            Pageable pageable = PageRequest.of(page, effectiveSize, Sort.by(Sort.Direction.DESC, "editedAt"));
            List<Snippet> recentSnippets = snippetService.getSnippetsByPosterId(user.getId(), pageable).getContent();
            model.addAttribute("recentSnippets", recentSnippets);
        }

        return "index";
    }
}
