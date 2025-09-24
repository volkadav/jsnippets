package com.norrisjackson.jsnippets.controllers;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@Slf4j
public class Login {
    @GetMapping("/login")
    String login(Model model) {
        return "login";
    }
}
