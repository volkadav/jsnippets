package com.norrisjackson.jsnippets.controllers;

import com.norrisjackson.jsnippets.services.UserService;
import com.norrisjackson.jsnippets.data.User;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class AuthPrincipalControllerAdvice {
    private final UserService userService;

    public AuthPrincipalControllerAdvice(UserService userService) {
        this.userService = userService;
    }

    @ModelAttribute("currentUser")
    public User getCurrentUser(@AuthenticationPrincipal org.springframework.security.core.userdetails.User principal) {
        if (principal == null) return null;

        return userService.getUserByUsername(principal.getUsername()).orElse(null);
    }
}
