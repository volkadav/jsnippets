package com.norrisjackson.jsnippets.controllers;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class SnippetsControllerTest {
    @Autowired
    private MockMvc mvc;

    @Test
    public void getSnippetsListUnathenticated() throws Exception {
        mvc.perform(MockMvcRequestBuilders.get("/snippets")
                .accept(MediaType.TEXT_HTML))
                .andExpect(status().isFound()) // 302 redirect expected
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @WithMockUser(username = "alice")
    public void getSnippetsListAuthenticated() throws Exception {
        mvc.perform(MockMvcRequestBuilders.get("/snippets")
                .accept(MediaType.TEXT_HTML))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Your Snippets")));
    }
}
