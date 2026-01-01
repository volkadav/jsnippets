package com.norrisjackson.jsnippets.configs;

import com.norrisjackson.jsnippets.security.JwtAuthenticationFilter;
import com.norrisjackson.jsnippets.security.RateLimitingFilter;
import com.norrisjackson.jsnippets.security.CustomAuthenticationFailureHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.annotation.web.configurers.LogoutConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final RateLimitingFilter rateLimitingFilter;
    private final CustomAuthenticationFailureHandler failureHandler;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter, RateLimitingFilter rateLimitingFilter, CustomAuthenticationFailureHandler failureHandler) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.rateLimitingFilter = rateLimitingFilter;
        this.failureHandler = failureHandler;
    }

    /**
     * AuthenticationManager bean for authentication
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    /**
     * Security filter chain for REST API endpoints
     * Uses JWT-based authentication with stateless sessions
     */
    @Bean
    @Order(1)
    public SecurityFilterChain apiFilterChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher("/api/**")
            .csrf(AbstractHttpConfigurer::disable) // Disable CSRF for stateless API
            .authorizeHttpRequests(authz -> authz
                    .requestMatchers("/api/auth/**", "/api/v1/auth/**").permitAll() // Allow authentication endpoints
                    .anyRequest().authenticated()) // All other API requests require authentication
            .sessionManagement(session -> session
                    .sessionCreationPolicy(SessionCreationPolicy.STATELESS)) // Stateless sessions for API
            .exceptionHandling(exceptions -> exceptions
                    .authenticationEntryPoint((request, response, authException) -> {
                        response.setStatus(401);
                        response.setContentType("application/json");
                        response.getWriter().write("{\"error\":\"Unauthorized\"}");
                    }))
            .headers(headers -> headers
                    .contentSecurityPolicy(csp -> csp
                            .policyDirectives("default-src 'none'; " +
                                    "script-src 'none'; " +
                                    "connect-src 'self'; " +
                                    "img-src 'none'; " +
                                    "style-src 'none'; " +
                                    "frame-ancestors 'none'"))
                    .frameOptions(HeadersConfigurer.FrameOptionsConfig::deny)
            )
            .addFilterBefore(rateLimitingFilter, UsernamePasswordAuthenticationFilter.class) // Add rate limiting first
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class); // Add JWT filter

        return http.build();
    }

    /**
     * Security filter chain for web UI endpoints
     * Uses form-based authentication with sessions
     */
    @Bean
    @Order(2)
    public SecurityFilterChain webFilterChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher("/**")
            .csrf(Customizer.withDefaults())
            .authorizeHttpRequests(authz -> authz
                    .requestMatchers("/", "/login", "/register",
                            "/webjars/**", "/css/**", "/js/**", "/images/**",
                            "/user/*/icon", "/user/*/icon/thumbnail",
                            "/favicon.ico", "/actuator", "/actuator/health/**", "/actuator/info").permitAll()
                    .anyRequest().authenticated())
            .formLogin(form -> form.loginPage("/login").failureHandler(failureHandler).permitAll())
            .logout(LogoutConfigurer::permitAll)
            .headers(headers -> headers
                    .contentSecurityPolicy(csp -> csp
                            .policyDirectives("default-src 'self'; " +
                                    "script-src 'self' 'unsafe-inline'; " + // unsafe-inline needed for Bootstrap JS
                                    "style-src 'self' 'unsafe-inline'; " +  // unsafe-inline needed for Bootstrap CSS
                                    "img-src 'self' data:; " + // data: for identicons/user icons
                                    "font-src 'self'; " +
                                    "connect-src 'self'; " +
                                    "frame-ancestors 'none'; " + // Prevent clickjacking
                                    "base-uri 'self'; " +
                                    "form-action 'self'"))
                    .frameOptions(HeadersConfigurer.FrameOptionsConfig::deny) // Prevent embedding in frames
            );
        return http.build();
    }
}
