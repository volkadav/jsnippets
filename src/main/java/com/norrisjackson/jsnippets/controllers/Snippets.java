package com.norrisjackson.jsnippets.controllers;

import com.norrisjackson.jsnippets.data.Snippet;
import com.norrisjackson.jsnippets.data.User;
import com.norrisjackson.jsnippets.services.SnippetService;

import lombok.extern.slf4j.Slf4j;
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

    public Snippets(SnippetService snippetService) {
        this.snippetService = snippetService;
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
    String viewSnippet(@PathVariable(name="id") Long id, Model model) {
        Snippet snippet = snippetService.getSnippetById(id).orElse(null);
        if (snippet == null) {
            log.warn("Snippet not found: id={}", id);
            return "redirect:/snippets";
        }

        model.addAttribute("snippet", snippet);
        return "snippet/view";
    }

    // Edit a specific snippet -- form display
    @GetMapping("/snippets/{id}/edit")
    String editSnippet(@PathVariable(name="id") Long id, Model model) throws Exception {
        Snippet snippet = snippetService.getSnippetById(id).orElse(null);
        if (snippet == null) {
            log.warn("Snippet not found: id={}", id);
            return "redirect:/snippets";
        }

        model.addAttribute("snippet", snippet);
        return "snippet/edit";
    }

    // Edit a specific snippet -- form handling
    @PostMapping("/snippets/{id}/edit")
    String handleEditSnippet(@PathVariable(name="id") Long id, @RequestParam String contents, Model model) {
        User currentUser = (User) model.getAttribute("currentUser");
        if (currentUser == null) {
            log.warn("No current user found in model");
            return "redirect:/login";
        }

        snippetService.updateSnippet(id, contents, currentUser);
        return "redirect:/snippets/" + id;
    }

    // Delete a specific snippet -- form display
    @GetMapping("/snippets/{id}/delete")
    String deleteSnippet(@PathVariable(name="id") Long id, Model model) {
        User currentUser = (User) model.getAttribute("currentUser");
        if (currentUser == null) {
            log.warn("No current user found");
            return "redirect:/login";
        }

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

    // Delete a specific snippet -- form handling
    @PostMapping("/snippets/{id}/delete")
    String handleDeleteSnippet(@PathVariable(name="id") Long id, Model model) {
        User currentUser = (User) model.getAttribute("currentUser");
        if (currentUser == null) {
            log.warn("No current user found in model");
            return "redirect:/login";
        }

        snippetService.deleteSnippet(id, currentUser);
        return "redirect:/snippets";
    }
}
