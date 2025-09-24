package com.norrisjackson.jsnippets.controllers;

import com.norrisjackson.jsnippets.data.User;
import com.norrisjackson.jsnippets.services.UserService;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Optional;

@Controller
@Slf4j
public class Index {
    private final UserService userService;

    public Index(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/")
    public String index(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Object principal = auth.getPrincipal();
        log.info("Principal: {}", principal);

        if (principal instanceof UserDetails authedUser) {
            Optional<User> userOpt = userService.getUserByUsername(authedUser.getUsername());
            if (userOpt.isEmpty()) {
                log.error("Authenticated user not found in database: {}", authedUser.getUsername());
                return "redirect:/logout";
            }
            User user = userOpt.get();

            model.addAttribute("greeting", "Welcome, " +
                    user.getUsername() + "!");
            model.addAttribute("username", user.getUsername());
            model.addAttribute("email", user.getEmail());
        }

        return "index";
    }
}
