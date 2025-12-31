package com.norrisjackson.jsnippets.controllers;

import com.norrisjackson.jsnippets.data.User;
import com.norrisjackson.jsnippets.services.UserService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;

@Controller
@Slf4j
public class Register {
    private final UserService userSvc;

    public Register(UserService userSvc) {
        this.userSvc = userSvc;
    }

    /**
     * Display the user registration page.
     * Error messages are passed via flash attributes from redirects.
     *
     * @param model the Spring MVC model
     * @return the register view name
     */
    @GetMapping("/register")
    String register(Model model) {
        // Flash attributes (error) are automatically added to the model by Spring
        return "register";
    }

    /**
     * Handle user registration form submission.
     *
     * @param username  the chosen username
     * @param password  the user's password
     * @param password2 password confirmation
     * @param email     the user's email address
     * @param redirectAttributes for flash messages
     * @return redirect URL (to login on success, back to register with error on failure)
     */
    @PostMapping("/register")
    String handleRegister(@RequestParam String username,
                          @RequestParam String password,
                          @RequestParam String password2,
                          @RequestParam String email,
                          RedirectAttributes redirectAttributes) {
        if (StringUtils.isBlank(username) || StringUtils.isBlank(password) ||
            StringUtils.isBlank(password2) || StringUtils.isBlank(email)) {
            redirectAttributes.addFlashAttribute("error", "Please fill in all fields.");
            return "redirect:/register";
        }
        if (!password.equals(password2)) {
            redirectAttributes.addFlashAttribute("error", "Passwords do not match.");
            return "redirect:/register";
        }
        if (password.length() < 8) {
            redirectAttributes.addFlashAttribute("error", "Password must be at least 8 characters.");
            return "redirect:/register";
        }
        if (userSvc.userExists(username)) {
            redirectAttributes.addFlashAttribute("error", "Username is already taken.");
            return "redirect:/register";
        }
        if (userSvc.emailExists(email)) {
            redirectAttributes.addFlashAttribute("error", "An account with that email already exists.");
            return "redirect:/register";
        }

        Optional<User> userOpt = userSvc.createUser(username, password, email);
        if (userOpt.isEmpty()) {
            log.warn("User creation failed!");
            redirectAttributes.addFlashAttribute("error", "An internal error occurred. Please try again.");
            return "redirect:/register";
        }
        User newUser = userOpt.get();

        log.info("Created new user: {}", newUser.getUsername());

        redirectAttributes.addFlashAttribute("success", "Account created successfully! Please log in.");
        return "redirect:/login";
    }
}
