package com.norrisjackson.jsnippets.controllers;

import com.google.common.base.Strings;
import com.norrisjackson.jsnippets.data.Snippet;
import com.norrisjackson.jsnippets.data.User;
import com.norrisjackson.jsnippets.services.SnippetService;

import com.norrisjackson.jsnippets.services.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
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

    public Snippets(SnippetService snippetService, UserService userService) {
        this.snippetService = snippetService;
        this.userService = userService;
    }

    // List all snippets
    @GetMapping({"/snippets", "/snippets/{username}"})
    String snippets(@PathVariable(name = "username", required = false) String username,
                    @RequestParam(name = "page", required = false) Integer page,
                    @RequestParam(name = "size", required = false) Integer size,
                    @RequestParam(name = "sort", required = false) String sortDir,
                    Model model) {
        User currentUser = (User) model.getAttribute("currentUser");
        if (currentUser == null) {
            log.warn("No current user found in model");
            return "redirect:/login";
        }
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

        List<Snippet> snippets = snippetService.getSnippetsByPosterId(viewUser.getId(), sort);
        long snippetCount = snippets.size();
        log.info("Found {} snippets for user {}", snippetCount, viewUser.getUsername());
        model.addAttribute("snippets", snippets);
        model.addAttribute("snippetCount", snippetCount);

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
    @GetMapping("/snippet/{id}")
    String viewSnippet(@PathVariable(name="id") Long id, Model model) {
        Snippet snippet = snippetService.getSnippetById(id).orElse(null);
        if (snippet == null) {
            log.warn("Snippet not found: id={}", id);
            return "redirect:/snippets";
        }

        model.addAttribute("snippet", snippet);

        User currentUser = (User) model.getAttribute("currentUser");
        if (currentUser == null) {
            log.warn("No current user found in model");
            return "redirect:/login";
        }
        model.addAttribute("isOwner",
                snippetService.userOwnsSnippet(snippet.getId(),
                currentUser.getId()));

        return "snippet/view";
    }

    // Edit a specific snippet -- form display
    @GetMapping("/snippet/{id}/edit")
    String editSnippet(@PathVariable(name="id") Long id, Model model) throws Exception {
        Snippet snippet = snippetService.getSnippetById(id).orElse(null);
        if (snippet == null) {
            log.warn("Snippet not found: id={}", id);
            return "redirect:/snippets";
        }

        User currentUser = (User) model.getAttribute("currentUser");
        if (currentUser == null) {
            log.warn("No current user found in model");
            return "redirect:/login";
        }
        if (!snippetService.userOwnsSnippet(id, currentUser.getId())) {
            log.warn("User {} attempted to edit snippet {} they do not own", currentUser.getUsername(), id);
            return "redirect:/snippets";
        }

        model.addAttribute("snippet", snippet);
        return "snippet/edit";
    }

    // Edit a specific snippet -- form handling
    @PostMapping("/snippet/{id}/edit")
    String handleEditSnippet(@PathVariable(name="id") Long id, @RequestParam String contents, Model model) {
        User currentUser = (User) model.getAttribute("currentUser");
        if (currentUser == null) {
            log.warn("No current user found in model");
            return "redirect:/login";
        }
        if (!snippetService.userOwnsSnippet(id, currentUser.getId())) {
            log.warn("User {} attempted to edit snippet {} they do not own", currentUser.getUsername(), id);
            return "redirect:/snippets";
        }
        if (Strings.isNullOrEmpty(contents)) {
            log.warn("Attempted to update snippet with empty contents");
            return "redirect:/snippet/" + id + "/edit";
        }

        snippetService.updateSnippet(id, contents, currentUser);
        return "redirect:/snippets/" + id;
    }

    // Delete a specific snippet -- form display
    @GetMapping("/snippet/{id}/delete")
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
    @PostMapping("/snippet/{id}/delete")
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
