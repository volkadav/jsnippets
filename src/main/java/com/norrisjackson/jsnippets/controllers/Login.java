package com.norrisjackson.jsnippets.controllers;

import com.norrisjackson.jsnippets.security.CustomAuthenticationFailureHandler;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import jakarta.servlet.http.HttpSession;

@Controller
public class Login {

    @GetMapping("/login")
    String login(HttpSession session, Model model) {
        // Check for error message from failed login attempt
        Object error = session.getAttribute(CustomAuthenticationFailureHandler.LOGIN_ERROR_KEY);
        if (error != null) {
            model.addAttribute("error", error.toString());
            session.removeAttribute(CustomAuthenticationFailureHandler.LOGIN_ERROR_KEY);
        }
        return "login";
    }
}
