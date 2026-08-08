package com.norrisjackson.jsnippets.controllers;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import static org.hamcrest.Matchers.containsString;

@SpringBootTest
@AutoConfigureMockMvc
public class IndexControllerTest {
    @Autowired
    private MockMvc mvc;

    @Test
    public void getIndex() throws Exception {
        mvc.perform(MockMvcRequestBuilders.get("/")
                .accept(MediaType.TEXT_HTML))
                .andExpect(status().isOk())
                .andExpect(xpath("//h1").exists());
    }

    @Test
    @WithMockUser(username = "alice")
    public void getIndexAsLoggedInUser() throws Exception {
        mvc.perform(MockMvcRequestBuilders.get("/")
                .accept(MediaType.TEXT_HTML))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("View Timeline")));
    }

    @Test
    @WithAnonymousUser
    public void getIndexAsAnonymousUser() throws Exception {
        mvc.perform(MockMvcRequestBuilders.get("/")
                .accept(MediaType.TEXT_HTML))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("log in")))
                .andExpect(content().string(containsString("register")));
    }

    @Test
    @WithMockUser(username = "alice")
    public void getIndexLoggedInNoFlash_NoGreeting() throws Exception {
        mvc.perform(MockMvcRequestBuilders.get("/")
                .accept(MediaType.TEXT_HTML))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("logged in as")))
                .andExpect(content().string(containsString("profile settings")))
                .andExpect(content().string(containsString("View Timeline")))
                .andExpect(content().string(containsString("Create New Snippet")))
                .andExpect(content().string(containsString("Browse Users")))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("see more"))))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("Welcome, alice!"))));
    }

    @Test
    @WithMockUser(username = "alice")
    public void getIndexWithWelcomeFlash_ShowsGreetingOnce() throws Exception {
        org.springframework.mock.web.MockHttpSession session =
                new org.springframework.mock.web.MockHttpSession();
        session.setAttribute(com.norrisjackson.jsnippets.security.CustomAuthenticationSuccessHandler.WELCOME_FLASH_KEY, Boolean.TRUE);

        // First view: greeting shown
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .get("/").session(session).accept(MediaType.TEXT_HTML))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Welcome, alice!")));

        // Second view: flash consumed, no greeting
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .get("/").session(session).accept(MediaType.TEXT_HTML))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("Welcome, alice!"))));
    }

    @Test
    @WithMockUser(username = "alice")
    public void getIndexLoggedIn_NoViewSnippetsAction() throws Exception {
        mvc.perform(MockMvcRequestBuilders.get("/")
                .accept(MediaType.TEXT_HTML))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("View your snippets"))));
    }

    @Test
    @WithMockUser(username = "charlie")
    public void getIndexWithManySnippets_ShowsSeeMore() throws Exception {
        mvc.perform(MockMvcRequestBuilders.get("/")
                .accept(MediaType.TEXT_HTML))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("see more")));
    }
}