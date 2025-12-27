package com.norrisjackson.jsnippets.controllers;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class LoginControllerTest {

    @Autowired
    private MockMvc mvc;

    @Test
    public void getLogin() throws Exception {
        mvc.perform(get("/login")
                .accept(MediaType.TEXT_HTML))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Log in")));
    }

    @Test
    public void loginWithValidCredentials() throws Exception {
        mvc.perform(MockMvcRequestBuilders.post("/login")
                .param("username", "alice")
                .param("password", "password")
                .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(MockMvcResultMatchers.status().is3xxRedirection())
                .andExpect(MockMvcResultMatchers.redirectedUrl("/"));
    }

    @Test
    public void loginWithInvalidCredentials() throws Exception {
        mvc.perform(MockMvcRequestBuilders.post("/login")
                .param("username", "alice")
                .param("password", "wrongpassword")
                .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(MockMvcResultMatchers.status().is3xxRedirection())
                .andExpect(MockMvcResultMatchers.redirectedUrl("/login"));
    }

    @Test
    public void loginWithNonexistentUser() throws Exception {
        mvc.perform(MockMvcRequestBuilders.post("/login")
                .param("username", "notarealuser")
                .param("password", "irrelevant")
                .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(MockMvcResultMatchers.status().is3xxRedirection())
                .andExpect(MockMvcResultMatchers.redirectedUrl("/login"));
    }

    @Test
    public void failedLoginShowsErrorMessage() throws Exception {
        // Perform failed login and capture the session
        MvcResult result = mvc.perform(MockMvcRequestBuilders.post("/login")
                .param("username", "alice")
                .param("password", "wrongpassword")
                .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(MockMvcResultMatchers.status().is3xxRedirection())
                .andExpect(MockMvcResultMatchers.redirectedUrl("/login"))
                .andReturn();

        MockHttpSession session = (MockHttpSession) result.getRequest().getSession(false);
        assert session != null;

        // Request login page with same session - error message should be displayed
        mvc.perform(get("/login").session(session).accept(MediaType.TEXT_HTML))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Invalid username or password.")));
    }
}
