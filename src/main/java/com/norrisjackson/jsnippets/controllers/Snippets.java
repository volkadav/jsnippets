package com.norrisjackson.jsnippets.controllers;

import com.norrisjackson.jsnippets.data.Snippet;
import com.norrisjackson.jsnippets.data.User;
import com.norrisjackson.jsnippets.services.SnippetService;
import com.norrisjackson.jsnippets.services.UserService;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
@Slf4j
public class Snippets {
    private final SnippetService snippetService;
    private final UserService userService;

    public Snippets(SnippetService snippetService,
                    UserService userService) {
        this.snippetService = snippetService;
        this.userService = userService;
    }

    // List all snippets
    @GetMapping("/snippets")
    String snippets(Model model) {
        User currentUser = (User) model.getAttribute("currentUser");
        if (currentUser == null) {
            log.warn("No current user found in model");
            return "redirect:/login";
        }

        List<Snippet> snippets = snippetService.getSnippetsByPosterId(currentUser.getId());
        model.addAttribute("snippets", snippets);

        return "snippet/list";
    }

    // Create a new snippet -- form display
    @GetMapping("/snippets/new")
    String newSnippet(Model model) {
        return "snippet/new";
    }

    // Create a new snippet -- form handling
    @PostMapping("/snippets/new")
    String handleNewSnippet(@RequestParam String contents, Model model) {
        User currentUser = (User) model.getAttribute("currentUser");
        if (currentUser == null) {
            log.warn("No current user found in model");
            return "redirect:/login";
        }

        snippetService.createSnippet(contents, currentUser);

        return "redirect:/snippets";
    }

    // View a specific snippet
    @GetMapping("/snippets/{id}")
    String viewSnippet(@RequestParam Long id, Model model) {
        return "snippet/view";
    }

    // Edit a specific snippet -- form display
    @GetMapping("/snippets/{id}/edit")
    String editSnippet(@RequestParam Long id, Model model) {
        return "snippet/edit";
    }

    // Edit a specific snippet -- form handling
    @PostMapping("/snippets/{id}/edit")
    String handleEditSnippet(@RequestParam Long id, @RequestParam String content) {
        return "redirect:/snippets/" + id;
    }

    // Delete a specific snippet -- form display
    @GetMapping("/snippets/{id}/delete")
    String deleteSnippet(@RequestParam Long id) {
        return "snippet/delete";
    }

    // Delete a specific snippet -- form handling
    @PostMapping("/snippets/{id}/delete")
    String handleDeleteSnippet(@RequestParam Long id) {
        return "redirect:/snippets";
    }
}
