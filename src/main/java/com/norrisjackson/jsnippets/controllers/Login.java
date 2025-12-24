package com.norrisjackson.jsnippets.controllers;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@Slf4j
public class Login {
    /**
     * Display the login page.
     *
     * @param model the Spring MVC model
     * @return the login view name
     */
    @GetMapping("/login")
    String login(Model model) {
        return "login";
    }
}
