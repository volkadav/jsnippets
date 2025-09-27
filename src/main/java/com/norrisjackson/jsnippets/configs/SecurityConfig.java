package com.norrisjackson.jsnippets.configs;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.LogoutConfigurer;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.password.Pbkdf2PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    private final UserDetailsService userDetailsService;

    public SecurityConfig(UserDetailsService userDetailsService) {
        this.userDetailsService = userDetailsService;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        // for now, leave everything open (todo: implement authn/authz stuff atop all this)
        http
            .formLogin(form -> form
                .loginPage("/login")
                .permitAll())
            .logout(LogoutConfigurer::permitAll)
            .authorizeHttpRequests(
                requests -> requests
                    .requestMatchers("/", "/login", "/register",
                            "/webjars/**", "/css/**", "/js/**", "/images/**",
                            "/favicon.ico").permitAll()
                    .anyRequest().authenticated())
            .userDetailsService(userDetailsService);
        return http.build();
    }
}
