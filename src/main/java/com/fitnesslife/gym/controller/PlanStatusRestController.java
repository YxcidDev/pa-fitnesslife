package com.fitnesslife.gym.controller;

import com.fitnesslife.gym.model.Payment;
import com.fitnesslife.gym.model.User;
import com.fitnesslife.gym.service.PaymentService;
import com.fitnesslife.gym.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/api/plan")
@RequiredArgsConstructor
public class PlanStatusRestController {

    private final PaymentService paymentService;
    private final UserService userService;

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getPlanStatus(
            @AuthenticationPrincipal UserDetails userDetails) {

        try {
            log.info("Consultando estado del plan para usuario: {}", userDetails.getUsername());

            User user = userService.getUserOrThrow(userDetails.getUsername());
            Optional<Payment> activePaymentOpt = paymentService.getActivePayment(user.getId());

            Map<String, Object> response = new HashMap<>();

            if (activePaymentOpt.isPresent()) {
                Payment activePayment = activePaymentOpt.get();
                long daysRemaining = ChronoUnit.DAYS.between(LocalDateTime.now(), activePayment.getValidUntil());

                response.put("hasActivePlan", true);
                response.put("planName", activePayment.getPlan().getPlanName());
                response.put("validFrom", activePayment.getValidFrom());
                response.put("validUntil", activePayment.getValidUntil());
                response.put("daysRemaining", daysRemaining);
                response.put("amount", activePayment.getAmount());
                response.put("currency", activePayment.getCurrency());
            } else {
                response.put("hasActivePlan", false);
                response.put("message", "No tienes un plan activo");
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error consultando estado del plan", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Error al consultar el estado del plan"));
        }
    }
}
