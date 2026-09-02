package com.fitnesslife.gym.service;

import java.security.SecureRandom;
import java.util.concurrent.TimeUnit;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import com.fitnesslife.gym.model.User;
import com.fitnesslife.gym.repository.UserRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private static final String OTP_PREFIX   = "otp:";
    private static final int    OTP_TTL_MIN  = 5;
    private static final int    OTP_LENGTH   = 6;

    private final StringRedisTemplate redisTemplate;
    private final JavaMailSender      mailSender;
    private final UserRepository      userRepository;
    private final PasswordEncoder     passwordEncoder;

    public boolean sendOtp(String email) {

        if (userRepository.findByEmail(email).isEmpty()) {
            log.warn("Intento de recuperación para email inexistente: {}", email);
            return false;
        }

        String otp = generateOtp();

        redisTemplate.opsForValue().set(
                OTP_PREFIX + email,
                otp,
                OTP_TTL_MIN,
                TimeUnit.MINUTES
        );

        log.info("OTP generado y guardado en Redis para: {}", email);

        try {
            sendOtpEmail(email, otp);
        } catch (MessagingException e) {
            log.error("Error enviando correo OTP a {}: {}", email, e.getMessage());
            redisTemplate.delete(OTP_PREFIX + email);
            throw new RuntimeException("No se pudo enviar el correo. Intenta de nuevo.");
        }

        return true;
    }

    public String verifyOtpAndChangePassword(String email, String otpIngresado, String newPassword) {
        String redisKey    = OTP_PREFIX + email;
        String otpGuardado = redisTemplate.opsForValue().get(redisKey);

        if (otpGuardado == null || !otpGuardado.equals(otpIngresado.trim())) {
            log.warn("OTP inválido o expirado para: {}", email);
            return "INVALID_OTP";
        }

        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            return "USER_NOT_FOUND";
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        redisTemplate.delete(redisKey);

        log.info("Contraseña actualizada exitosamente para: {}", email);
        return "OK";
    }

    private String generateOtp() {
        SecureRandom random = new SecureRandom();
        int otp = 100_000 + random.nextInt(900_000);
        return String.valueOf(otp);
    }

    private void sendOtpEmail(String toEmail, String otp) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setTo(toEmail);
        helper.setSubject("Fitness Life – Tu código de recuperación");
        helper.setFrom("fitnesslifectg@gmail.com");

        String html = buildEmailHtml(otp);
        helper.setText(html, true);

        mailSender.send(message);
        log.info("Correo OTP enviado a: {}", toEmail);
    }

    private String buildEmailHtml(String otp) {
        return """
            <!DOCTYPE html>
            <html lang="es">
            <head><meta charset="UTF-8"></head>
            <body style="margin:0;padding:0;background:#1d1b1a;font-family:'Work Sans',Arial,sans-serif;">
              <table width="100%%" cellpadding="0" cellspacing="0"
                     style="max-width:520px;margin:40px auto;background:#323333;border-radius:8px;overflow:hidden;">
                <tr>
                  <td style="background:#eb6608;padding:28px 32px;">
                    <h1 style="margin:0;color:#fff;font-size:22px;font-family:Montserrat,Arial,sans-serif;">
                      Fitness Life
                    </h1>
                  </td>
                </tr>
                <tr>
                  <td style="padding:36px 32px;">
                    <p style="color:#d3d3d3;font-size:15px;margin:0 0 12px;">
                      Recibimos una solicitud para restablecer tu contraseña.
                    </p>
                    <p style="color:#d3d3d3;font-size:15px;margin:0 0 28px;">
                      Usa el siguiente código. <strong style="color:#fff;">Expira en 5 minutos.</strong>
                    </p>
                    <div style="text-align:center;margin:0 0 28px;">
                      <span style="display:inline-block;background:#eb6608;color:#fff;
                                   font-size:36px;font-weight:700;letter-spacing:10px;
                                   padding:16px 32px;border-radius:6px;font-family:monospace;">
                        %s
                      </span>
                    </div>
                    <p style="color:#888;font-size:12px;margin:0;">
                      Si no solicitaste este cambio, ignora este correo. Tu contraseña no será modificada.
                    </p>
                  </td>
                </tr>
                <tr>
                  <td style="background:#1d1b1a;padding:16px 32px;text-align:center;">
                    <p style="color:#555;font-size:11px;margin:0;">
                      © 2025 Fitness Life – Todos los derechos reservados
                    </p>
                  </td>
                </tr>
              </table>
            </body>
            </html>
            """.formatted(otp);
    }
}


