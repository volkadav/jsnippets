package com.norrisjackson.jsnippets.controllers;

import com.norrisjackson.jsnippets.data.User;
import com.norrisjackson.jsnippets.services.UserService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.ZoneId;
import java.util.Set;
import java.util.TreeSet;

@Controller
@Slf4j
public class Profile {
    private final UserService userService;

    public Profile(UserService userService) {
        this.userService = userService;
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/profile")
    String profile(@RequestParam(required = false) String success,
                   @RequestParam(required = false) String error,
                   Model model) {
        User currentUser = (User) model.getAttribute("currentUser");

        // Add success and error messages if present
        if (!StringUtils.isBlank(success)) {
            switch (success) {
                case "updated" -> model.addAttribute("success", "Profile updated successfully!");
                default -> model.addAttribute("success", "Operation completed successfully.");
            }
        }

        if (!StringUtils.isBlank(error)) {
            switch (error) {
                case "emptyfields" -> model.addAttribute("error", "Please fill in all required fields.");
                case "invalidemail" -> model.addAttribute("error", "Please enter a valid email address.");
                case "invalidtimezone" -> model.addAttribute("error", "Please select a valid timezone.");
                case "emailexists" -> model.addAttribute("error", "An account with that email already exists.");
                case "internalerror" -> model.addAttribute("error", "An internal error occurred. Please try again.");
                default -> model.addAttribute("error", "An error occurred.");
            }
        }

        // Get available timezones for the dropdown
        Set<String> availableTimezones = new TreeSet<>(ZoneId.getAvailableZoneIds());
        model.addAttribute("availableTimezones", availableTimezones);
        model.addAttribute("user", currentUser);

        return "profile";
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/profile")
    String handleProfileUpdate(@RequestParam String email,
                               @RequestParam String timezone,
                               Model model) {
        User currentUser = (User) model.getAttribute("currentUser");

        // Validate input
        if (StringUtils.isBlank(email) || StringUtils.isBlank(timezone)) {
            return "redirect:/profile?error=emptyfields";
        }

        // Basic email validation
        if (!email.contains("@") || !email.contains(".")) {
            return "redirect:/profile?error=invalidemail";
        }

        // Validate timezone
        try {
            ZoneId.of(timezone);
        } catch (Exception e) {
            return "redirect:/profile?error=invalidtimezone";
        }

        // Check if email is already taken by another user
        if (!email.equals(currentUser.getEmail()) && userService.emailExists(email)) {
            return "redirect:/profile?error=emailexists";
        }

        // Update user profile
        try {
            currentUser.setEmail(email);
            currentUser.setTimezone(timezone);
            
            // We need to add an update method to UserService
            userService.updateUser(currentUser);
            
            log.info("Profile updated for user: {}", currentUser.getUsername());
            return "redirect:/profile?success=updated";
        } catch (Exception e) {
            log.error("Error updating profile for user: {}", currentUser.getUsername(), e);
            return "redirect:/profile?error=internalerror";
        }
    }
}