package com.norrisjackson.jsnippets.controllers;

import com.norrisjackson.jsnippets.configs.PaginationConfig;
import com.norrisjackson.jsnippets.data.Snippet;
import com.norrisjackson.jsnippets.data.User;
import com.norrisjackson.jsnippets.security.CustomAuthenticationSuccessHandler;
import com.norrisjackson.jsnippets.services.SnippetService;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
@Slf4j
public class Index {
    private final SnippetService snippetService;
    private final PaginationConfig paginationConfig;

    public Index(SnippetService snippetService, PaginationConfig paginationConfig) {
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
                        HttpSession session,
                        Model model) {
        User currentUser = (User) model.getAttribute("currentUser");

        if (currentUser != null) {
            model.addAttribute("username", currentUser.getUsername());
            model.addAttribute("currentUser", currentUser);
            model.addAttribute("snippetCount",
                    snippetService.getSnippetCountByPosterId(currentUser.getId()));

            // Show the welcome flash only on the first view after login
            if (Boolean.TRUE.equals(session.getAttribute(CustomAuthenticationSuccessHandler.WELCOME_FLASH_KEY))) {
                session.removeAttribute(CustomAuthenticationSuccessHandler.WELCOME_FLASH_KEY);
                model.addAttribute("greeting", "Welcome, " +
                        currentUser.getUsername() + "!");
            }

            if (page == null || page < 0) page = 0;
            int effectiveSize = paginationConfig.getEffectivePageSize(size);
            model.addAttribute("currentPage", page);
            model.addAttribute("pageSize", effectiveSize);

            Pageable pageable = PageRequest.of(page, effectiveSize, Sort.by(Sort.Direction.DESC, "editedAt"));
            List<Snippet> recentSnippets = snippetService.getSnippetsByPosterId(currentUser.getId(), pageable).getContent();
            model.addAttribute("recentSnippets", recentSnippets);
        }

        return "index";
    }
}
