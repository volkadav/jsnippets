package com.norrisjackson.jsnippets.controllers;

import com.norrisjackson.jsnippets.controllers.dto.RegistrationRequest;
import com.norrisjackson.jsnippets.data.User;
import com.norrisjackson.jsnippets.services.UserService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
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
     * @param request the registration request DTO with validation
     * @param bindingResult validation results
     * @param redirectAttributes for flash messages
     * @return redirect URL (to login on success, back to register with error on failure)
     */
    @PostMapping("/register")
    String handleRegister(@Valid RegistrationRequest request,
                          BindingResult bindingResult,
                          RedirectAttributes redirectAttributes) {
        // Check for validation errors
        if (bindingResult.hasErrors()) {
            String errorMessage = bindingResult.getFieldErrors().stream()
                    .map(error -> error.getDefaultMessage())
                    .findFirst()
                    .orElse("Please correct the errors in the form.");
            redirectAttributes.addFlashAttribute("error", errorMessage);
            return "redirect:/register";
        }

        // Check password confirmation
        if (!request.password().equals(request.password2())) {
            redirectAttributes.addFlashAttribute("error", "Passwords do not match.");
            return "redirect:/register";
        }

        // Check for existing username
        if (userSvc.userExists(request.username())) {
            redirectAttributes.addFlashAttribute("error", "Username is already taken.");
            return "redirect:/register";
        }

        // Check for existing email
        if (userSvc.emailExists(request.email())) {
            redirectAttributes.addFlashAttribute("error", "An account with that email already exists.");
            return "redirect:/register";
        }

        Optional<User> userOpt = userSvc.createUser(request.username(), request.password(), request.email());
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
