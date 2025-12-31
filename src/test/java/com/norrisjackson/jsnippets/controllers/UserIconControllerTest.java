package com.norrisjackson.jsnippets.controllers;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests for UserIconController functionality.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserIconControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void getUserIcon_returnsImage() throws Exception {
        mockMvc.perform(get("/user/alice/icon"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_PNG));
    }

    @Test
    void getUserIconThumbnail_returnsImage() throws Exception {
        mockMvc.perform(get("/user/alice/icon/thumbnail"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_PNG));
    }

    @Test
    void getUserIcon_nonexistentUser_returns404() throws Exception {
        mockMvc.perform(get("/user/nonexistentuser/icon"))
                .andExpect(status().isNotFound());
    }

    @Test
    void uploadIcon_requiresAuthentication() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "icon", "test.png", "image/png", new byte[100]);

        mockMvc.perform(multipart("/profile/icon").file(file))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "alice")
    void uploadIcon_withValidImage_succeeds() throws Exception {
        // Create a minimal valid PNG (1x1 transparent pixel)
        byte[] pngData = createMinimalPng();
        MockMultipartFile file = new MockMultipartFile(
                "icon", "test.png", "image/png", pngData);

        mockMvc.perform(multipart("/profile/icon")
                        .file(file)
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profile"))
                .andExpect(flash().attributeExists("success"));
    }

    @Test
    @WithMockUser(username = "alice")
    void uploadIcon_withEmptyFile_returnsError() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "icon", "test.png", "image/png", new byte[0]);

        mockMvc.perform(multipart("/profile/icon")
                        .file(file)
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profile"))
                .andExpect(flash().attributeExists("error"));
    }

    @Test
    @WithMockUser(username = "alice")
    void uploadIcon_withInvalidContentType_returnsError() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "icon", "test.txt", "text/plain", "not an image".getBytes());

        mockMvc.perform(multipart("/profile/icon")
                        .file(file)
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profile"))
                .andExpect(flash().attributeExists("error"));
    }

    @Test
    @WithMockUser(username = "alice")
    void uploadIcon_withFileTooLarge_returnsError() throws Exception {
        // Create a file larger than 32KB
        byte[] largeData = new byte[33 * 1024];
        MockMultipartFile file = new MockMultipartFile(
                "icon", "large.png", "image/png", largeData);

        mockMvc.perform(multipart("/profile/icon")
                        .file(file)
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profile"))
                .andExpect(flash().attributeExists("error"));
    }

    @Test
    @WithMockUser(username = "alice")
    void removeIcon_succeeds() throws Exception {
        mockMvc.perform(post("/profile/icon/remove")
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profile"))
                .andExpect(flash().attributeExists("success"));
    }

    @Test
    void removeIcon_requiresAuthentication() throws Exception {
        mockMvc.perform(post("/profile/icon/remove")
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    /**
     * Create a minimal valid PNG image (1x1 transparent pixel).
     */
    private byte[] createMinimalPng() {
        // Minimal PNG header and data for a 1x1 transparent pixel
        return new byte[] {
            (byte)0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, // PNG signature
            0x00, 0x00, 0x00, 0x0D, // IHDR chunk length
            0x49, 0x48, 0x44, 0x52, // "IHDR"
            0x00, 0x00, 0x00, 0x01, // width: 1
            0x00, 0x00, 0x00, 0x01, // height: 1
            0x08, 0x06, // 8-bit RGBA
            0x00, 0x00, 0x00, // compression, filter, interlace
            0x1F, 0x15, (byte)0xC4, (byte)0x89, // IHDR CRC
            0x00, 0x00, 0x00, 0x0A, // IDAT chunk length
            0x49, 0x44, 0x41, 0x54, // "IDAT"
            0x78, (byte)0x9C, 0x63, 0x00, 0x01, 0x00, 0x00, 0x05, 0x00, 0x01, // compressed data
            0x0D, 0x0A, 0x2D, (byte)0xB4, // IDAT CRC
            0x00, 0x00, 0x00, 0x00, // IEND chunk length
            0x49, 0x45, 0x4E, 0x44, // "IEND"
            (byte)0xAE, 0x42, 0x60, (byte)0x82 // IEND CRC
        };
    }
}

