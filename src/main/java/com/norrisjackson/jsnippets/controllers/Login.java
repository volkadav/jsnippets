package com.norrisjackson.jsnippets.controllers;

import com.google.common.base.Strings;
import com.norrisjackson.jsnippets.data.User;
import com.norrisjackson.jsnippets.services.UserService;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.context.request.RequestContextHolder;

@Controller
@Slf4j
public class Login {

    private final UserService userSvc;

    public Login(UserService userSvc) {
       this.userSvc = userSvc;
    }

    @GetMapping("/login")
    String login(@RequestParam(required = false) String error, Model model) {
        if (!Strings.isNullOrEmpty(error)) {
            switch (error) {
                case "invalidcredentials" -> model.addAttribute("error", "Invalid username or password.");
                case "missingcredentials" -> model.addAttribute("error", "Please enter both username and password.");
                default -> model.addAttribute("error", "An unknown error occurred.");
            }
        }

        return "login";
    }

    @PostMapping("/login")
    String handleLogin(@RequestParam String username,
                       @RequestParam String password,
                       HttpSession session) {
        log.info("Login attempt for user: {}", username);
        if (Strings.isNullOrEmpty(username) || Strings.isNullOrEmpty(password)) {
            return "redirect:/login?error=missingcredentials";
        }
        if (!userSvc.authenticateUser(username, password)) {
            return "redirect:/login?error=invalidcredentials";
        }

        User user = userSvc.getUserByUsername(username);
        log.info("User {} logged in successfully", user);

        session.setAttribute("userid", user.getId());
        session.setAttribute("username", user.getName());

        return "redirect:/";
    }
}
