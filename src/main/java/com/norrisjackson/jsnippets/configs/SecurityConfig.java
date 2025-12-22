package com.norrisjackson.jsnippets.configs;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.LogoutConfigurer;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    @SuppressWarnings("unused")
    public SecurityFilterChain apiFilterChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher("/api/**")
            .csrf(csrf -> csrf.ignoringRequestMatchers("/api/**"))
            .authorizeHttpRequests(authz -> authz.anyRequest().authenticated())
            .httpBasic(httpBasic -> httpBasic.realmName("JSnippets API"));
        return http.build();
    }

    @Bean
    public SecurityFilterChain webFilterChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher("/**")
            .csrf(Customizer.withDefaults())
            .authorizeHttpRequests(authz -> authz
                    .requestMatchers("/", "/login", "/register",
                            "/webjars/**", "/css/**", "/js/**", "/images/**",
                            "/favicon.ico", "/actuator", "/actuator/health/**", "/actuator/info").permitAll()
                    .anyRequest().authenticated())
            .formLogin(form -> form.loginPage("/login").permitAll())
            .logout(LogoutConfigurer::permitAll);
        return http.build();
    }
}
