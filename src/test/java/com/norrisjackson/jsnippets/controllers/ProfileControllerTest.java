package com.norrisjackson.jsnippets.controllers;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests for Profile controller functionality.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProfileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void viewOwnProfile_requiresAuthentication() throws Exception {
        mockMvc.perform(get("/profile"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @WithMockUser(username = "alice")
    void viewOwnProfile_whenAuthenticated_showsProfilePage() throws Exception {
        mockMvc.perform(get("/profile").accept(MediaType.TEXT_HTML))
                .andExpect(status().isOk())
                .andExpect(view().name("profile"))
                .andExpect(model().attributeExists("user"))
                .andExpect(model().attribute("isOwnProfile", true));
    }

    @Test
    @WithMockUser(username = "alice")
    void viewOtherUserProfile_showsFollowButton() throws Exception {
        mockMvc.perform(get("/profile/bob").accept(MediaType.TEXT_HTML))
                .andExpect(status().isOk())
                .andExpect(view().name("profile"))
                .andExpect(model().attributeExists("user"))
                .andExpect(model().attribute("isOwnProfile", false))
                .andExpect(model().attributeExists("isFollowing"))
                .andExpect(model().attributeExists("followerCount"))
                .andExpect(model().attributeExists("followingCount"));
    }

    @Test
    @WithMockUser(username = "alice")
    void viewOwnProfileByUsername_redirectsToProfile() throws Exception {
        mockMvc.perform(get("/profile/alice"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profile"));
    }

    @Test
    @WithMockUser(username = "alice")
    void viewNonexistentUserProfile_redirectsWithError() throws Exception {
        mockMvc.perform(get("/profile/nonexistentuser"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profile"))
                .andExpect(flash().attributeExists("error"));
    }

    @Test
    @WithMockUser(username = "alice")
    void followUser_success() throws Exception {
        mockMvc.perform(post("/profile/bob/follow")
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profile/bob"))
                .andExpect(flash().attributeExists("success"));
    }

    @Test
    @WithMockUser(username = "alice")
    void followNonexistentUser_redirectsWithError() throws Exception {
        mockMvc.perform(post("/profile/nonexistentuser/follow")
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profile"))
                .andExpect(flash().attributeExists("error"));
    }

    @Test
    @WithMockUser(username = "alice")
    void unfollowUser_whenNotFollowing_redirectsWithError() throws Exception {
        mockMvc.perform(post("/profile/charlie/unfollow")
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profile/charlie"))
                .andExpect(flash().attributeExists("error"));
    }
}

