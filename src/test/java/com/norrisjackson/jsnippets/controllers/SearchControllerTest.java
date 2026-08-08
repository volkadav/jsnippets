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
class SearchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void search_requiresAuth() throws Exception {
        mockMvc.perform(get("/search"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @WithMockUser(username = "alice")
    void search_withoutQuery_showsPrompt() throws Exception {
        mockMvc.perform(get("/search").accept(MediaType.TEXT_HTML))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Enter a term")));
    }

    @Test
    @WithMockUser(username = "alice")
    void search_withQuery_renders() throws Exception {
        mockMvc.perform(get("/search").param("q", "test").accept(MediaType.TEXT_HTML))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Search")));
    }

    @Test
    @WithMockUser(username = "alice")
    void search_rendersBothSections() throws Exception {
        mockMvc.perform(get("/search").param("q", "alice").accept(MediaType.TEXT_HTML))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString(">Users</h2>")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(">Snippets</h2>")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("alice")));
    }
}
