package com.norrisjackson.jsnippets.controllers;

import com.norrisjackson.jsnippets.data.User;
import com.norrisjackson.jsnippets.services.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.google.common.base.Strings;

import java.util.Optional;

@Controller
@Slf4j
public class Register {
    private UserService userSvc;

    public Register(UserService userSvc) {
        this.userSvc = userSvc;
    }

    @GetMapping("/register")
    String register(@RequestParam(required = false) String error, Model model) {
        if (!Strings.isNullOrEmpty(error)) {
            switch (error) {
                case "emptyfields" -> model.addAttribute("error", "Please fill in all fields.");
                case "passwordmismatch" -> model.addAttribute("error", "Passwords do not match.");
                case "passwordtooshort" -> model.addAttribute("error", "Password must be at least 8 characters.");
                case "userexists" -> model.addAttribute("error", "Username is already taken.");
                case "emailexists" -> model.addAttribute("error", "An account with that email already exists.");
                case "internalerror" -> model.addAttribute("error", "An internal error occurred. Please try again.");
                default -> model.addAttribute("error", "An unknown error occurred.");
            }
        }

        return "register";
    }

    @PostMapping("/register")
    String handleRegister(@RequestParam String username,
                          @RequestParam String password,
                          @RequestParam String password2,
                          @RequestParam String email) {
        if (Strings.isNullOrEmpty(username) || Strings.isNullOrEmpty(password) ||
            Strings.isNullOrEmpty(password2) || Strings.isNullOrEmpty(email)) {
            return "redirect:/register?error=emptyfields";
        }
        if (!password.equals(password2)) {
            return "redirect:/register?error=passwordmismatch";
        }
        if (password.length() < 8) {
            return "redirect:/register?error=passwordtooshort";
        }
        if (userSvc.userExists(username)) {
            return "redirect:/register?error=userexists";
        }
        if (userSvc.emailExists(email)) {
            return "redirect:/register?error=emailexists";
        }

        Optional<User> userOpt = userSvc.createUser(username, password, email);
        if (userOpt.isEmpty()) {
            log.warn("User creation failed!");
            return "redirect:/register?error=internalerror";
        }
        User newUser = userOpt.get();

        log.info("Created new user: {}", newUser.getUsername());

        return "redirect:/login";
    }
}
