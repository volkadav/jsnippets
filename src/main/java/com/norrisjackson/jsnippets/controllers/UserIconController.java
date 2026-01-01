package com.norrisjackson.jsnippets.controllers;

import com.norrisjackson.jsnippets.configs.UserIconConfig;
import com.norrisjackson.jsnippets.data.User;
import com.norrisjackson.jsnippets.services.IdenticonService;
import com.norrisjackson.jsnippets.services.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Controller for handling user icon/avatar operations.
 */
@Controller
@Slf4j
public class UserIconController {

    private final UserService userService;
    private final IdenticonService identiconService;
    private final UserIconConfig iconConfig;

    public UserIconController(UserService userService, IdenticonService identiconService, UserIconConfig iconConfig) {
        this.userService = userService;
        this.identiconService = identiconService;
        this.iconConfig = iconConfig;
    }

    /**
     * Serve a user's icon image at full size.
     * Falls back to an identicon if no custom icon is set.
     *
     * @param username the username
     * @return the icon image
     */
    @GetMapping("/user/{username}/icon")
    public ResponseEntity<byte[]> getUserIcon(@PathVariable String username) {
        return getUserIconWithSize(username, iconConfig.getFullSize());
    }

    /**
     * Serve a user's icon image as a thumbnail (32x32).
     * Falls back to an identicon if no custom icon is set.
     *
     * @param username the username
     * @return the thumbnail icon image
     */
    @GetMapping("/user/{username}/icon/thumbnail")
    public ResponseEntity<byte[]> getUserIconThumbnail(@PathVariable String username) {
        return getUserIconWithSize(username, iconConfig.getThumbnailSize());
    }

    private ResponseEntity<byte[]> getUserIconWithSize(String username, int size) {
        Optional<User> userOpt = userService.getUserByUsername(username);
        if (userOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        User user = userOpt.get();
        byte[] imageData;
        String contentType;

        if (user.getIcon() != null && user.getIconContentType() != null) {
            // User has a custom icon
            if (size == iconConfig.getFullSize()) {
                imageData = user.getIcon();
            } else {
                // Resize for thumbnail
                imageData = identiconService.resizeImage(user.getIcon(), size, size);
            }
            contentType = size == iconConfig.getFullSize() ? user.getIconContentType() : "image/png";
        } else {
            // Generate identicon from email
            imageData = identiconService.generateIdenticon(user.getEmail(), size);
            contentType = "image/png";
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .cacheControl(CacheControl.maxAge(1, TimeUnit.HOURS).cachePublic())
                .body(imageData);
    }

    /**
     * Upload a new icon for the current user.
     *
     * @param file the icon file
     * @param model the Spring MVC model
     * @param redirectAttributes for flash messages
     * @return redirect to profile page
     */
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/profile/icon")
    public String uploadIcon(@RequestParam("icon") MultipartFile file,
                             Model model,
                             RedirectAttributes redirectAttributes) {
        User currentUser = (User) model.getAttribute("currentUser");
        if (currentUser == null) {
            return "redirect:/login";
        }

        // Validate file
        if (file.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Please select an image file to upload.");
            return "redirect:/profile";
        }

        String contentType = file.getContentType();
        if (contentType == null || !iconConfig.getAllowedContentTypes().contains(contentType)) {
            redirectAttributes.addFlashAttribute("error",
                    "Invalid file type. Please upload a PNG, JPEG, GIF, or WebP image.");
            return "redirect:/profile";
        }

        if (file.getSize() > iconConfig.getMaxSize()) {
            redirectAttributes.addFlashAttribute("error",
                    String.format("Image file is too large. Maximum size is %d bytes.", iconConfig.getMaxSize()));
            return "redirect:/profile";
        }

        try {
            byte[] iconData = file.getBytes();
            currentUser.setIcon(iconData);
            currentUser.setIconContentType(contentType);
            userService.updateUser(currentUser);

            log.info("Icon uploaded for user: {}", currentUser.getUsername());
            redirectAttributes.addFlashAttribute("success", "Profile icon updated successfully!");
        } catch (IOException e) {
            log.error("Error uploading icon for user: {}", currentUser.getUsername(), e);
            redirectAttributes.addFlashAttribute("error", "Failed to upload icon. Please try again.");
        }

        return "redirect:/profile";
    }

    /**
     * Remove the current user's custom icon (will fall back to identicon).
     *
     * @param model the Spring MVC model
     * @param redirectAttributes for flash messages
     * @return redirect to profile page
     */
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/profile/icon/remove")
    public String removeIcon(Model model, RedirectAttributes redirectAttributes) {
        User currentUser = (User) model.getAttribute("currentUser");
        if (currentUser == null) {
            return "redirect:/login";
        }

        currentUser.setIcon(null);
        currentUser.setIconContentType(null);
        userService.updateUser(currentUser);

        log.info("Icon removed for user: {}", currentUser.getUsername());
        redirectAttributes.addFlashAttribute("success", "Profile icon removed. Using default icon.");

        return "redirect:/profile";
    }
}

