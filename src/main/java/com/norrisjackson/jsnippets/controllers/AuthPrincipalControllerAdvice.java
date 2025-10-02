package com.norrisjackson.jsnippets.controllers;

import com.norrisjackson.jsnippets.services.UserService;
import com.norrisjackson.jsnippets.services.TimezoneService;
import com.norrisjackson.jsnippets.data.User;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class AuthPrincipalControllerAdvice {
    private final UserService userService;
    private final TimezoneService timezoneService;

    public AuthPrincipalControllerAdvice(UserService userService, TimezoneService timezoneService) {
        this.userService = userService;
        this.timezoneService = timezoneService;
    }

    @ModelAttribute("currentUser")
    public User getCurrentUser(@AuthenticationPrincipal org.springframework.security.core.userdetails.User principal) {
        if (principal == null) return null;

        return userService.getUserByUsername(principal.getUsername()).orElse(null);
    }

    @ModelAttribute("timezoneService")
    public TimezoneService getTimezoneService() {
        return timezoneService;
    }
}
