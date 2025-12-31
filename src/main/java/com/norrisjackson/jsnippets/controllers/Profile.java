package com.norrisjackson.jsnippets.controllers;

import com.norrisjackson.jsnippets.data.User;
import com.norrisjackson.jsnippets.services.UserService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.ZoneId;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

@Controller
@Slf4j
public class Profile {
    private final UserService userService;

    public Profile(UserService userService) {
        this.userService = userService;
    }

    /**
     * Display the current user's profile page (editable).
     *
     * @param success optional success message parameter
     * @param error   optional error message parameter
     * @param model   the Spring MVC model
     * @return the profile view name
     */
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
                case "followed" -> model.addAttribute("success", "You are now following this user.");
                case "unfollowed" -> model.addAttribute("success", "You have unfollowed this user.");
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
                case "cannotfollowself" -> model.addAttribute("error", "You cannot follow yourself.");
                case "alreadyfollowing" -> model.addAttribute("error", "You are already following this user.");
                case "notfollowing" -> model.addAttribute("error", "You are not following this user.");
                case "usernotfound" -> model.addAttribute("error", "User not found.");
                default -> model.addAttribute("error", "An error occurred.");
            }
        }

        // Get available timezones for the dropdown
        Set<String> availableTimezones = new TreeSet<>(ZoneId.getAvailableZoneIds());
        model.addAttribute("availableTimezones", availableTimezones);
        model.addAttribute("user", currentUser);
        model.addAttribute("isOwnProfile", true);

        return "profile";
    }

    /**
     * Display another user's profile page (read-only with follow button).
     *
     * @param username the username of the profile to view
     * @param success  optional success message parameter
     * @param error    optional error message parameter
     * @param model    the Spring MVC model
     * @return the profile view name or redirect if viewing own profile
     */
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/profile/{username}")
    String viewUserProfile(@PathVariable String username,
                           @RequestParam(required = false) String success,
                           @RequestParam(required = false) String error,
                           Model model) {
        User currentUser = (User) model.getAttribute("currentUser");

        // If viewing own profile, redirect to editable profile page
        if (currentUser != null && currentUser.getUsername().equals(username)) {
            return "redirect:/profile";
        }

        // Find the user to view
        Optional<User> profileUserOpt = userService.getUserByUsername(username);
        if (profileUserOpt.isEmpty()) {
            return "redirect:/profile?error=usernotfound";
        }

        User profileUser = profileUserOpt.get();

        // Add success and error messages if present
        if (!StringUtils.isBlank(success)) {
            switch (success) {
                case "followed" -> model.addAttribute("success", "You are now following " + profileUser.getUsername() + ".");
                case "unfollowed" -> model.addAttribute("success", "You have unfollowed " + profileUser.getUsername() + ".");
                default -> model.addAttribute("success", "Operation completed successfully.");
            }
        }

        if (!StringUtils.isBlank(error)) {
            switch (error) {
                case "cannotfollowself" -> model.addAttribute("error", "You cannot follow yourself.");
                case "alreadyfollowing" -> model.addAttribute("error", "You are already following this user.");
                case "notfollowing" -> model.addAttribute("error", "You are not following this user.");
                case "internalerror" -> model.addAttribute("error", "An internal error occurred. Please try again.");
                default -> model.addAttribute("error", "An error occurred.");
            }
        }

        // Check if current user is following this profile user
        boolean isFollowing = userService.isFollowing(currentUser, profileUser);

        model.addAttribute("user", profileUser);
        model.addAttribute("isOwnProfile", false);
        model.addAttribute("isFollowing", isFollowing);
        model.addAttribute("followerCount", userService.getFollowerCount(profileUser));
        model.addAttribute("followingCount", userService.getFollowingCount(profileUser));

        return "profile";
    }

    /**
     * Handle follow action.
     *
     * @param username the username of the user to follow
     * @param model    the Spring MVC model
     * @return redirect URL
     */
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/profile/{username}/follow")
    String followUser(@PathVariable String username, Model model) {
        User currentUser = (User) model.getAttribute("currentUser");
        if (currentUser == null) {
            return "redirect:/login";
        }

        Optional<User> toFollowOpt = userService.getUserByUsername(username);
        if (toFollowOpt.isEmpty()) {
            return "redirect:/profile?error=usernotfound";
        }

        User toFollow = toFollowOpt.get();

        try {
            boolean success = userService.followUser(currentUser, toFollow);
            if (success) {
                log.info("User {} followed {}", currentUser.getUsername(), username);
                return "redirect:/profile/" + username + "?success=followed";
            } else {
                // Either already following or trying to follow self
                if (currentUser.equals(toFollow)) {
                    return "redirect:/profile/" + username + "?error=cannotfollowself";
                }
                return "redirect:/profile/" + username + "?error=alreadyfollowing";
            }
        } catch (Exception e) {
            log.error("Error following user {} by {}", username, currentUser.getUsername(), e);
            return "redirect:/profile/" + username + "?error=internalerror";
        }
    }

    /**
     * Handle unfollow action.
     *
     * @param username the username of the user to unfollow
     * @param model    the Spring MVC model
     * @return redirect URL
     */
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/profile/{username}/unfollow")
    String unfollowUser(@PathVariable String username, Model model) {
        User currentUser = (User) model.getAttribute("currentUser");
        if (currentUser == null) {
            return "redirect:/login";
        }

        Optional<User> toUnfollowOpt = userService.getUserByUsername(username);
        if (toUnfollowOpt.isEmpty()) {
            return "redirect:/profile?error=usernotfound";
        }

        User toUnfollow = toUnfollowOpt.get();

        try {
            boolean success = userService.unfollowUser(currentUser, toUnfollow);
            if (success) {
                log.info("User {} unfollowed {}", currentUser.getUsername(), username);
                return "redirect:/profile/" + username + "?success=unfollowed";
            } else {
                return "redirect:/profile/" + username + "?error=notfollowing";
            }
        } catch (Exception e) {
            log.error("Error unfollowing user {} by {}", username, currentUser.getUsername(), e);
            return "redirect:/profile/" + username + "?error=internalerror";
        }
    }

    /**
     * Handle profile update form submission.
     *
     * @param email the updated email address
     * @param timezone the updated timezone
     * @param model the Spring MVC model
     * @return redirect URL
     */
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
            
            userService.updateUser(currentUser);
            
            log.info("Profile updated for user: {}", currentUser.getUsername());
            return "redirect:/profile?success=updated";
        } catch (Exception e) {
            log.error("Error updating profile for user: {}", currentUser.getUsername(), e);
            return "redirect:/profile?error=internalerror";
        }
    }
}