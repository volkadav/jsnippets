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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;


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
     * Success and error messages are passed via flash attributes from redirects.
     *
     * @param model the Spring MVC model
     * @return the profile view name
     */
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/profile")
    String profile(Model model) {
        User currentUser = (User) model.getAttribute("currentUser");

        // Flash attributes (success/error) are automatically added to the model by Spring

        // Get available timezones for the dropdown
        Set<String> availableTimezones = new TreeSet<>(ZoneId.getAvailableZoneIds());
        model.addAttribute("availableTimezones", availableTimezones);
        model.addAttribute("user", currentUser);
        model.addAttribute("isOwnProfile", true);

        return "profile";
    }

    /**
     * Display another user's profile page (read-only with follow button).
     * Success and error messages are passed via flash attributes from redirects.
     *
     * @param username the username of the profile to view
     * @param model    the Spring MVC model
     * @param redirectAttributes for flash messages if redirect needed
     * @return the profile view name or redirect if viewing own profile
     */
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/profile/{username}")
    String viewUserProfile(@PathVariable String username,
                           Model model,
                           RedirectAttributes redirectAttributes) {
        User currentUser = (User) model.getAttribute("currentUser");

        // If viewing own profile, redirect to editable profile page
        if (currentUser != null && currentUser.getUsername().equals(username)) {
            return "redirect:/profile";
        }

        // Find the user to view
        Optional<User> profileUserOpt = userService.getUserByUsername(username);
        if (profileUserOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "User not found.");
            return "redirect:/profile";
        }

        User profileUser = profileUserOpt.get();

        // Flash attributes (success/error) are automatically added to the model by Spring

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
     * @param redirectAttributes for flash messages
     * @return redirect URL
     */
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/profile/{username}/follow")
    String followUser(@PathVariable String username,
                      Model model,
                      RedirectAttributes redirectAttributes) {
        User currentUser = (User) model.getAttribute("currentUser");
        if (currentUser == null) {
            return "redirect:/login";
        }

        Optional<User> toFollowOpt = userService.getUserByUsername(username);
        if (toFollowOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "User not found.");
            return "redirect:/profile";
        }

        User toFollow = toFollowOpt.get();

        try {
            boolean success = userService.followUser(currentUser, toFollow);
            if (success) {
                log.info("User {} followed {}", currentUser.getUsername(), username);
                redirectAttributes.addFlashAttribute("success", "You are now following " + username + ".");
            } else {
                // Either already following or trying to follow self
                if (currentUser.getId().equals(toFollow.getId())) {
                    redirectAttributes.addFlashAttribute("error", "You cannot follow yourself.");
                } else {
                    redirectAttributes.addFlashAttribute("error", "You are already following this user.");
                }
            }
        } catch (Exception e) {
            log.error("Error following user {} by {}", username, currentUser.getUsername(), e);
            redirectAttributes.addFlashAttribute("error", "An internal error occurred. Please try again.");
        }
        return "redirect:/profile/" + username;
    }

    /**
     * Handle unfollow action.
     *
     * @param username the username of the user to unfollow
     * @param model    the Spring MVC model
     * @param redirectAttributes for flash messages
     * @return redirect URL
     */
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/profile/{username}/unfollow")
    String unfollowUser(@PathVariable String username,
                        Model model,
                        RedirectAttributes redirectAttributes) {
        User currentUser = (User) model.getAttribute("currentUser");
        if (currentUser == null) {
            return "redirect:/login";
        }

        Optional<User> toUnfollowOpt = userService.getUserByUsername(username);
        if (toUnfollowOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "User not found.");
            return "redirect:/profile";
        }

        User toUnfollow = toUnfollowOpt.get();

        try {
            boolean success = userService.unfollowUser(currentUser, toUnfollow);
            if (success) {
                log.info("User {} unfollowed {}", currentUser.getUsername(), username);
                redirectAttributes.addFlashAttribute("success", "You have unfollowed " + username + ".");
            } else {
                redirectAttributes.addFlashAttribute("error", "You are not following this user.");
            }
        } catch (Exception e) {
            log.error("Error unfollowing user {} by {}", username, currentUser.getUsername(), e);
            redirectAttributes.addFlashAttribute("error", "An internal error occurred. Please try again.");
        }
        return "redirect:/profile/" + username;
    }

    /**
     * Handle profile update form submission.
     *
     * @param email the updated email address
     * @param timezone the updated timezone
     * @param bio the updated bio (optional)
     * @param model the Spring MVC model
     * @param redirectAttributes for flash messages
     * @return redirect URL
     */
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/profile")
    String handleProfileUpdate(@RequestParam String email,
                               @RequestParam String timezone,
                               @RequestParam(required = false) String bio,
                               Model model,
                               RedirectAttributes redirectAttributes) {
        User currentUser = (User) model.getAttribute("currentUser");

        // Validate input
        if (StringUtils.isBlank(email) || StringUtils.isBlank(timezone)) {
            redirectAttributes.addFlashAttribute("error", "Please fill in all required fields.");
            return "redirect:/profile";
        }

        // Basic email validation
        if (!email.contains("@") || !email.contains(".")) {
            redirectAttributes.addFlashAttribute("error", "Please enter a valid email address.");
            return "redirect:/profile";
        }

        // Validate timezone
        try {
            ZoneId.of(timezone);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Please select a valid timezone.");
            return "redirect:/profile";
        }

        // Check if email is already taken by another user
        if (!email.equals(currentUser.getEmail()) && userService.emailExists(email)) {
            redirectAttributes.addFlashAttribute("error", "An account with that email already exists.");
            return "redirect:/profile";
        }

        // Validate bio length
        if (bio != null && bio.length() > 4000) {
            redirectAttributes.addFlashAttribute("error", "Bio must be 4000 characters or less.");
            return "redirect:/profile";
        }

        // Update user profile
        try {
            currentUser.setEmail(email);
            currentUser.setTimezone(timezone);
            
            // Sanitize bio - strip HTML tags and escape special characters
            if (bio != null) {
                // Remove HTML tags
                String sanitizedBio = bio.replaceAll("<[^>]*>", "");
                // Trim whitespace
                sanitizedBio = sanitizedBio.trim();
                // Set to null if empty after sanitization
                currentUser.setBio(sanitizedBio.isEmpty() ? null : sanitizedBio);
            } else {
                currentUser.setBio(null);
            }

            userService.updateUser(currentUser);
            
            log.info("Profile updated for user: {}", currentUser.getUsername());
            redirectAttributes.addFlashAttribute("success", "Profile updated successfully!");
        } catch (Exception e) {
            log.error("Error updating profile for user: {}", currentUser.getUsername(), e);
            redirectAttributes.addFlashAttribute("error", "An internal error occurred. Please try again.");
        }
        return "redirect:/profile";
    }
}

