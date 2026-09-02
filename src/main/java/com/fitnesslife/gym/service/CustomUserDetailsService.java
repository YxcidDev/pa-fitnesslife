package com.fitnesslife.gym.service;

import com.fitnesslife.gym.model.User;
import com.fitnesslife.gym.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Slf4j
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepo;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        log.info("Intentando autenticar usuario con email: {}", email);
        
        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> {
                    log.error("Usuario no encontrado en MongoDB: {}", email);
                    return new UsernameNotFoundException("User not found: " + email);
                });

        log.debug("Usuario encontrado. Roles: {}", user.getRole());

        return org.springframework.security.core.userdetails.User
                .withUsername(user.getEmail())
                .password(user.getPassword())
                .roles(user.getRole().name()) 
                .disabled(!user.isActive()) 
                .build();
    }
}
