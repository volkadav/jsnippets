package com.norrisjackson.jsnippets.controllers;

import com.norrisjackson.jsnippets.data.User;
import com.norrisjackson.jsnippets.services.TimezoneService;
import com.norrisjackson.jsnippets.services.UserService;
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

    /**
     * Add the currently authenticated user to the model for all controllers.
     *
     * @param principal the authenticated user principal
     * @return the User entity, or null if not authenticated
     */
    @ModelAttribute("currentUser")
    public User getCurrentUser(@AuthenticationPrincipal org.springframework.security.core.userdetails.User principal) {
        if (principal == null) return null;

        return userService.getUserByUsername(principal.getUsername()).orElse(null);
    }

    /**
     * Add the timezone service to the model for all controllers.
     *
     * @return the TimezoneService instance
     */
    @ModelAttribute("timezoneService")
    public TimezoneService getTimezoneService() {
        return timezoneService;
    }
}
