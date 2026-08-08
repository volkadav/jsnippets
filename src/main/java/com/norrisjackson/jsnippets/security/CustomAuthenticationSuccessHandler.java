package com.norrisjackson.jsnippets.security;

import com.norrisjackson.jsnippets.data.User;
import com.norrisjackson.jsnippets.services.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;
import java.util.Optional;

/**
 * Updates the user's lastLoggedInAt timestamp on successful authentication.
 */
@Component
@Slf4j
public class CustomAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    /**
     * Session attribute set on successful login and consumed (removed) on the
     * first authenticated view, so the welcome flash only shows once.
     */
    public static final String WELCOME_FLASH_KEY = "welcomeAfterLogin";

    private final UserService userService;

    public CustomAuthenticationSuccessHandler(UserService userService) {
        this.userService = userService;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                       Authentication authentication) throws IOException {
        // Update last login timestamp
        if (authentication.getPrincipal() instanceof UserDetails userDetails) {
            String username = userDetails.getUsername();
            Optional<User> userOpt = userService.getUserByUsername(username);

            if (userOpt.isPresent()) {
                User user = userOpt.get();
                user.setLastLoggedInAt(Instant.now());
                userService.updateUser(user);
                log.debug("Updated last login time for user: {}", username);
            }
        }

        // Flag the next view to show the one-time welcome flash
        request.getSession().setAttribute(WELCOME_FLASH_KEY, Boolean.TRUE);

        // Redirect to default success URL (home page)
        response.sendRedirect(request.getContextPath() + "/");
    }
}

