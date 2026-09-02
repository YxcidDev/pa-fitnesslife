package com.fitnesslife.gym.service;

import com.fitnesslife.gym.model.User;
import com.fitnesslife.gym.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

    @Mock private StringRedisTemplate redisTemplate;
    @Mock private ValueOperations<String, String> valueOperations;
    @Mock private JavaMailSender mailSender;
    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks
    private PasswordResetService passwordResetService;

    @Test
    void sendOtp_shouldReturnFalse_whenUserDoesNotExist() {
        when(userRepository.findByEmail("notfound@test.com")).thenReturn(Optional.empty());

        boolean result = passwordResetService.sendOtp("notfound@test.com");

        assertEquals(false, result);
        verifyNoInteractions(redisTemplate);
    }

    @Test
    void verifyOtp_shouldReturnInvalidOtp_whenCodeDoesNotMatch() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("otp:user@test.com")).thenReturn("123456");

        String result = passwordResetService.verifyOtpAndChangePassword(
                "user@test.com", "000000", "newPass123");

        assertEquals("INVALID_OTP", result);
        verify(userRepository, never()).save(any());
    }

    @Test
    void verifyOtp_shouldUpdatePassword_whenCodeIsCorrect() {
        User user = mock(User.class);

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("otp:user@test.com")).thenReturn("123456");
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("newPass123")).thenReturn("hashedPassword");

        String result = passwordResetService.verifyOtpAndChangePassword(
                "user@test.com", "123456", "newPass123");

        assertEquals("OK", result);
        verify(user).setPassword("hashedPassword");
        verify(userRepository).save(user);
        verify(redisTemplate).delete("otp:user@test.com");
    }
}