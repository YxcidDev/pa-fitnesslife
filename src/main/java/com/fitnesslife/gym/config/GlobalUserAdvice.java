package com.fitnesslife.gym.config;

import com.fitnesslife.gym.service.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.ui.Model;

@ControllerAdvice
public class GlobalUserAdvice {

    private final UserService userService;

    public GlobalUserAdvice(UserService userService) {
        this.userService = userService;
    }

    @ModelAttribute
    public void addCurrentUser(Model model, Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated()) {
            userService.findByEmail(authentication.getName())
                    .ifPresent(user -> model.addAttribute("currentUser", user));
        }
    }
}
