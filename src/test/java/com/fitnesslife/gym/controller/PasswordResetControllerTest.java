package com.fitnesslife.gym.controller;

import com.fitnesslife.gym.service.PasswordResetService;
import com.fitnesslife.gym.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PasswordResetController.class)
@AutoConfigureMockMvc(addFilters = false)
class PasswordResetControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PasswordResetService passwordResetService;

    @MockitoBean
    private UserService userService;

    @Test
    void sendOtp_shouldReturnOkTrue_whenServiceConfirmsSending() throws Exception {
        when(passwordResetService.sendOtp("user@test.com")).thenReturn(true);

        mockMvc.perform(post("/api/password/send-otp")
                        .param("email", "user@test.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ok").value(true));
    }

    @Test
    void resetPassword_shouldReject_whenPasswordIsTooShort() throws Exception {
        mockMvc.perform(post("/api/password/reset")
                        .param("email", "user@test.com")
                        .param("otp", "123456")
                        .param("newPassword", "abc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ok").value(false))
                .andExpect(jsonPath("$.msg").value("La contraseña debe tener al menos 6 caracteres."));
    }

    @Test
    void resetPassword_shouldConfirmOk_whenServiceReturnsOk() throws Exception {
        when(passwordResetService.verifyOtpAndChangePassword("user@test.com", "123456", "newPass123"))
                .thenReturn("OK");

        mockMvc.perform(post("/api/password/reset")
                        .param("email", "user@test.com")
                        .param("otp", "123456")
                        .param("newPassword", "newPass123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ok").value(true));
    }
}