package com.norrisjackson.jsnippets.controllers;

import com.norrisjackson.jsnippets.data.User;
import com.norrisjackson.jsnippets.services.TimezoneService;
import com.norrisjackson.jsnippets.services.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class AuthPrincipalControllerAdvice {
    private static final String CACHED_USER_ATTR = AuthPrincipalControllerAdvice.class.getName() + ".currentUser";

    private final UserService userService;
    private final TimezoneService timezoneService;

    public AuthPrincipalControllerAdvice(UserService userService, TimezoneService timezoneService) {
        this.userService = userService;
        this.timezoneService = timezoneService;
    }

    /**
     * Add the currently authenticated user to the model for all controllers.
     * Result is cached in the request to avoid redundant DB queries.
     *
     * @param principal the authenticated user principal
     * @param request   the current HTTP request (for per-request caching)
     * @return the User entity, or null if not authenticated
     */
    @ModelAttribute("currentUser")
    public User getCurrentUser(@AuthenticationPrincipal org.springframework.security.core.userdetails.User principal,
                               HttpServletRequest request) {
        if (principal == null) return null;

        // Return cached user if already resolved in this request
        Object cached = request.getAttribute(CACHED_USER_ATTR);
        if (cached instanceof User cachedUser) {
            return cachedUser;
        }

        User user = userService.getUserByUsername(principal.getUsername()).orElse(null);
        if (user != null) {
            request.setAttribute(CACHED_USER_ATTR, user);
        }
        return user;
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
