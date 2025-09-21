package com.norrisjackson.jsnippets.controllers;

import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@Slf4j
public class Index {
    @GetMapping("/")
    public String index(Model model, HttpSession session) {
        log.info("Accessing index page, session ID: {}", session.getId());
        log.info("Session attributes: userid={}, username={}",
                session.getAttribute("userid"),
                session.getAttribute("username"));

        if (session.getAttribute("username") != null) {
            model.addAttribute("message", "Welcome, " +
                    session.getAttribute("username") + "!");
        } else {
            model.addAttribute("message",
                    "Welcome, guest! Please log in or register.");
        }

        return "index";
    }
}
