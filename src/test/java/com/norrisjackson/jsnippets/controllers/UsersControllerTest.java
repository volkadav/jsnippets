package com.norrisjackson.jsnippets.controllers;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.test.context.support.WithMockUser;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UsersControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void users_requiresAuth() throws Exception {
        mockMvc.perform(get("/users"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @WithMockUser(username = "alice")
    void users_directory_renders() throws Exception {
        mockMvc.perform(get("/users").accept(MediaType.TEXT_HTML))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Users")));
    }

    @Test
    @WithMockUser(username = "alice")
    void users_search_renders() throws Exception {
        mockMvc.perform(get("/users").param("q", "alice").accept(MediaType.TEXT_HTML))
                .andExpect(status().isOk());
    }
}
