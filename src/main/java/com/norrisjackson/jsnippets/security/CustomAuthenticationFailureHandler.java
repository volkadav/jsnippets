package com.norrisjackson.jsnippets.security;

import org.springframework.security.authentication.AccountExpiredException;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Stores a human-readable error message in the session and redirects to /login.
 * The Login controller reads and removes the message, passing it to the view.
 */
@Component
public class CustomAuthenticationFailureHandler implements AuthenticationFailureHandler {

    public static final String LOGIN_ERROR_KEY = "loginError";

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException exception) throws IOException {
        String message;

        if (exception instanceof DisabledException) {
            message = "Your account is disabled.";
        } else if (exception instanceof LockedException) {
            message = "Your account is locked.";
        } else if (exception instanceof CredentialsExpiredException) {
            message = "Your credentials have expired.";
        } else if (exception instanceof AccountExpiredException) {
            message = "Your account has expired.";
        } else {
            message = "Invalid username or password.";
        }

        request.getSession().setAttribute(LOGIN_ERROR_KEY, message);
        response.sendRedirect("/login");
    }
}
