package com.fitnesslife.gym.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import com.fitnesslife.gym.service.CustomUserDetailsService;

@Configuration
public class SecurityConfig {

    @Autowired
    private CustomUserDetailsService userDetailsService;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/", "/index", "/login", "/register",
                                "/css/**", "/js/**", "/img/**", "/fonts/**",
                                "/payment/confirmation", "/payment/response",
                                "/api/payment/status/**", "/ws/**", "/api/password/**")
                        .permitAll()

                        .requestMatchers("/dashboard/**", "/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/churn/**").hasRole("ADMIN")
                        .requestMatchers("/attendance/kiosk").hasRole("ADMIN")
                        .requestMatchers("/api/attendance/verify").hasRole("ADMIN")
                        .requestMatchers("/api/attendance/active-count").hasAnyRole("USER", "ADMIN")
                        .requestMatchers("/trainer/**").hasAnyRole("ADMIN", "TRAINER")
                        .requestMatchers("/client/**").hasAnyRole("USER", "TRAINER")
                        .requestMatchers("/api/plan/**").hasRole("USER")

                        .anyRequest().authenticated())

                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .usernameParameter("email")
                        .passwordParameter("password")
                        .successHandler((request, response, authentication) -> {
                            var authorities = authentication.getAuthorities();

                            if (authorities.stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
                                response.sendRedirect(request.getContextPath() + "/dashboard");
                                return;
                            }

                            if (authorities.stream().anyMatch(a -> a.getAuthority().equals("ROLE_USER"))) {
                                response.sendRedirect(request.getContextPath() + "/home");
                                return;
                            }

                            response.sendRedirect(request.getContextPath() + "/home");
                        })
                        .failureUrl("/login?error=true")
                        .permitAll())

                .sessionManagement(session -> session
                        .invalidSessionUrl("/login?expired=true")
                        .maximumSessions(1)
                        .maxSessionsPreventsLogin(false)
                        .sessionRegistry(sessionRegistry()))

                .logout(logout -> logout
                        .logoutSuccessUrl("/")
                        .permitAll())

                .csrf(csrf -> csrf.disable());

        return http.build();
    }

    @Bean
    public AuthenticationManager authManager(HttpSecurity http) throws Exception {
        AuthenticationManagerBuilder authenticationManagerBuilder = http
                .getSharedObject(AuthenticationManagerBuilder.class);

        authenticationManagerBuilder
                .userDetailsService(userDetailsService)
                .passwordEncoder(passwordEncoder());

        return authenticationManagerBuilder.build();
    }

    @Bean
    public SessionRegistry sessionRegistry() {
        return new SessionRegistryImpl();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
