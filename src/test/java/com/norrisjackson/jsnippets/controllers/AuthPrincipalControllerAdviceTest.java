package com.norrisjackson.jsnippets.controllers;

import com.norrisjackson.jsnippets.data.User;
import com.norrisjackson.jsnippets.services.TimezoneService;
import com.norrisjackson.jsnippets.services.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthPrincipalControllerAdviceTest {

    @Mock
    private UserService userService;

    @Mock
    private TimezoneService timezoneService;

    private AuthPrincipalControllerAdvice advice;
    private MockHttpServletRequest request;

    @BeforeEach
    void setUp() {
        advice = new AuthPrincipalControllerAdvice(userService, timezoneService);
        request = new MockHttpServletRequest();
    }

    @Test
    void getCurrentUser_nullPrincipal_returnsNull() {
        User result = advice.getCurrentUser(null, request);
        assertThat(result).isNull();
        verifyNoInteractions(userService);
    }

    @Test
    void getCurrentUser_validPrincipal_returnsUser() {
        var principal = new org.springframework.security.core.userdetails.User(
                "testuser", "pass", Collections.emptyList());
        User expectedUser = new User();
        expectedUser.setUsername("testuser");
        when(userService.getUserByUsername("testuser")).thenReturn(Optional.of(expectedUser));

        User result = advice.getCurrentUser(principal, request);

        assertThat(result).isNotNull();
        assertThat(result.getUsername()).isEqualTo("testuser");
        verify(userService).getUserByUsername("testuser");
    }

    @Test
    void getCurrentUser_userNotFound_returnsNull() {
        var principal = new org.springframework.security.core.userdetails.User(
                "unknown", "pass", Collections.emptyList());
        when(userService.getUserByUsername("unknown")).thenReturn(Optional.empty());

        User result = advice.getCurrentUser(principal, request);

        assertThat(result).isNull();
    }

    @Test
    void getCurrentUser_cachesResultInRequest() {
        var principal = new org.springframework.security.core.userdetails.User(
                "testuser", "pass", Collections.emptyList());
        User expectedUser = new User();
        expectedUser.setUsername("testuser");
        when(userService.getUserByUsername("testuser")).thenReturn(Optional.of(expectedUser));

        // First call — hits the database
        User first = advice.getCurrentUser(principal, request);
        // Second call — should use cache
        User second = advice.getCurrentUser(principal, request);

        assertThat(first).isSameAs(second);
        verify(userService, times(1)).getUserByUsername("testuser");
    }

    @Test
    void getCurrentUser_differentRequests_queryDatabaseAgain() {
        var principal = new org.springframework.security.core.userdetails.User(
                "testuser", "pass", Collections.emptyList());
        User expectedUser = new User();
        expectedUser.setUsername("testuser");
        when(userService.getUserByUsername("testuser")).thenReturn(Optional.of(expectedUser));

        advice.getCurrentUser(principal, request);
        MockHttpServletRequest secondRequest = new MockHttpServletRequest();
        advice.getCurrentUser(principal, secondRequest);

        verify(userService, times(2)).getUserByUsername("testuser");
    }

    @Test
    void getCurrentUser_nullUserNotCached() {
        var principal = new org.springframework.security.core.userdetails.User(
                "unknown", "pass", Collections.emptyList());
        when(userService.getUserByUsername("unknown")).thenReturn(Optional.empty());

        advice.getCurrentUser(principal, request);
        advice.getCurrentUser(principal, request);

        // DB queried both times since null was not cached
        verify(userService, times(2)).getUserByUsername("unknown");
    }

    @Test
    void getTimezoneService_returnsInjectedInstance() {
        TimezoneService result = advice.getTimezoneService();
        assertThat(result).isSameAs(timezoneService);
    }
}

