package com.norrisjackson.jsnippets.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class Login {
    @GetMapping("/login")
    String login() {
        return "login";
    }
}
