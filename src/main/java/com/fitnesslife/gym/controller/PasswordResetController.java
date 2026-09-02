package com.fitnesslife.gym.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.fitnesslife.gym.service.PasswordResetService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/password")
@RequiredArgsConstructor
public class PasswordResetController {

    private final PasswordResetService passwordResetService;

    @PostMapping("/send-otp")
    public ResponseEntity<Map<String, Object>> sendOtp(@RequestParam String email) {
        log.info("Solicitud de OTP para: {}", email);
        try {
            boolean sent = passwordResetService.sendOtp(email);

            if (!sent) {
                return ResponseEntity.ok(Map.of(
                        "ok",  false,
                        "msg", "Si el correo está registrado, recibirás el código en breve."
                ));
            }

            return ResponseEntity.ok(Map.of("ok", true));

        } catch (RuntimeException e) {
            log.error("Error en sendOtp: {}", e.getMessage());
            return ResponseEntity.ok(Map.of(
                    "ok",  false,
                    "msg", "Ocurrió un error al enviar el correo. Intenta de nuevo."
            ));
        }
    }

    @PostMapping("/reset")
    public ResponseEntity<Map<String, Object>> resetPassword(
            @RequestParam String email,
            @RequestParam String otp,
            @RequestParam String newPassword) {

        log.info("Intento de reset de contraseña para: {}", email);

        if (newPassword == null || newPassword.trim().length() < 6) {
            return ResponseEntity.ok(Map.of(
                    "ok",  false,
                    "msg", "La contraseña debe tener al menos 6 caracteres."
            ));
        }

        String result = passwordResetService.verifyOtpAndChangePassword(email, otp, newPassword);

        return switch (result) {
            case "OK" -> ResponseEntity.ok(Map.of("ok", true));

            case "INVALID_OTP" -> ResponseEntity.ok(Map.of(
                    "ok",  false,
                    "msg", "Código incorrecto o expirado. Solicita uno nuevo."
            ));

            case "USER_NOT_FOUND" -> ResponseEntity.ok(Map.of(
                    "ok",  false,
                    "msg", "No se encontró una cuenta con ese correo."
            ));

            default -> ResponseEntity.ok(Map.of(
                    "ok",  false,
                    "msg", "Error inesperado. Intenta de nuevo."
            ));
        };
    }
}


