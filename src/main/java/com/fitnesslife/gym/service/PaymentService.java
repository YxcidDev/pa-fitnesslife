package com.fitnesslife.gym.service;

import com.fitnesslife.gym.enums.PaymentStatus;
import com.fitnesslife.gym.model.Payment;
import com.fitnesslife.gym.model.Plan;
import com.fitnesslife.gym.model.User;
import com.fitnesslife.gym.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final UserService userService;
    private final PlanService planService;

    @Value("${epayco.public-key:}")
    private String epaycoPublicKey;

    @Value("${epayco.private-key:}")
    private String epaycoPrivateKey;

    @Value("${epayco.customer-id:}")
    private String epaycoCustomerId;

    @Transactional
    public Payment createPendingPayment(String userId, String planId, String externalInvoice) {
        log.info("Creando pago pendiente para usuario: {} y plan: {}", userId, planId);

        User user = userService.getUserByIdOrThrow(userId);
        Plan plan = planService.getPlanById(planId);

        Payment payment = Payment.builder()
                .user(user)
                .plan(plan)
                .externalInvoice(externalInvoice)
                .amount(plan.getPrice())
                .currency(plan.getCurrency())
                .status(PaymentStatus.PENDING)
                .userName(user.getName() + " " + user.getLastname())
                .userEmail(user.getEmail())
                .planName(plan.getPlanName())
                .build();

        Payment saved = paymentRepository.save(payment);
        log.info("Pago pendiente creado con ID: {}", saved.getId());
        return saved;
    }

    @Transactional
    public Payment processPaymentConfirmation(Map<String, String> params) {
        log.info("Procesando confirmación de pago para ref_payco: {}", params.get("x_ref_payco"));

        String refPayco = params.get("x_ref_payco");
        String externalInvoiceId = params.get("x_id_invoice");
        String transactionId = params.get("x_transaction_id");
        String amount = params.get("x_amount");
        String currencyCode = params.get("x_currency_code");
        String signature = params.get("x_signature");
        String responseCode = params.get("x_cod_response");
        String responseText = params.get("x_response");
        String responseReason = params.get("x_response_reason_text");
        String approvalCode = params.get("x_approval_code");
        String bankName = params.get("x_bank_name");
        String franchise = params.get("x_franchise");
        String transactionDate = params.get("x_transaction_date");

        String expectedSignature = generateSignature(refPayco, transactionId, amount, currencyCode);

        log.debug("DEBUG HASH: Cadena construida: {}^{}^{}^{}^{}^{} | Hash calculado: {} | Hash Recibido: {}",
                epaycoCustomerId, epaycoPrivateKey, refPayco, transactionId, amount, currencyCode,
                expectedSignature, signature);

        if (!expectedSignature.equals(signature)) {
            log.error("Firma inválida. Esperada: {}, Recibida: {}", expectedSignature, signature);
            throw new RuntimeException("Firma de pago inválida");
        }

        Payment payment = paymentRepository.findByExternalInvoice(externalInvoiceId)
                .orElseThrow(() -> new RuntimeException("Pago no encontrado: " + externalInvoiceId));

        if (!payment.getAmount().equals(Double.parseDouble(amount))) {
            log.error("Monto no coincide. Esperado: {}, Recibido: {}", payment.getAmount(), amount);
            throw new RuntimeException("El monto del pago no coincide");
        }

        PaymentStatus status = determineStatus(responseCode);
        payment.setStatus(status);
        payment.setTransactionId(transactionId);
        payment.setApprovalCode(approvalCode);
        payment.setBankName(bankName);
        payment.setFranchise(franchise);
        payment.setResponseCode(responseCode);
        payment.setResponseText(responseText);
        payment.setResponseReason(responseReason);
        payment.setSignature(signature);
        payment.setTransactionDate(parseTransactionDate(transactionDate));

        if (PaymentStatus.ACCEPTED.equals(status)) {
            LocalDateTime now = LocalDateTime.now();
            payment.setValidFrom(now);
            payment.setValidUntil(now.plusDays(payment.getPlan().getDurationDays()));

            User user = payment.getUser();
            user.setPlan(payment.getPlan().getPlanName());
            userService.save(user);

            log.info("Pago aceptado. Usuario {} actualizado con plan {}", user.getId(), user.getPlan());
        }

        Payment saved = paymentRepository.save(payment);
        log.info("Pago procesado con estado: {}", saved.getStatus());
        return saved;
    }

    private String generateSignature(String refPayco, String transactionId, String amount, String currency) {
        String data = epaycoCustomerId + "^" + epaycoPrivateKey + "^" + refPayco + "^" +
                transactionId + "^" + amount + "^" + currency;

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1)
                    hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            log.error("Error generando firma SHA256", e);
            throw new RuntimeException("Error generando firma", e);
        }
    }

    private PaymentStatus determineStatus(String responseCode) {
        return switch (responseCode) {
            case "1" -> PaymentStatus.ACCEPTED;
            case "2" -> PaymentStatus.REJECTED;
            case "3" -> PaymentStatus.PENDING;
            case "4" -> PaymentStatus.FAILED;
            default -> PaymentStatus.UNKNOWN;
        };
    }

    private LocalDateTime parseTransactionDate(String dateStr) {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            return LocalDateTime.parse(dateStr, formatter);
        } catch (Exception e) {
            log.warn("Error parseando fecha de transacción: {}", dateStr);
            return LocalDateTime.now();
        }
    }

    public List<Payment> getUserPayments(String userId) {
        User user = userService.getUserByIdOrThrow(userId);
        return paymentRepository.findByUser(user);
    }

    public Optional<Payment> getActivePayment(String userId) {
        User user = userService.getUserByIdOrThrow(userId);
        LocalDateTime now = LocalDateTime.now();

        List<Payment> activePayments = paymentRepository
                .findByUserAndStatusAndValidFromLessThanEqualAndValidUntilGreaterThanEqual(
                        user, PaymentStatus.ACCEPTED, now, now);

        return activePayments.isEmpty() ? Optional.empty() : Optional.of(activePayments.get(0));
    }

    public Optional<Payment> getPaymentByExternalInvoice(String externalInvoice) {
        return paymentRepository.findByExternalInvoice(externalInvoice);
    }

    public Optional<Payment> getPaymentByTransactionId(String transactionId) {
        return paymentRepository.findByTransactionId(transactionId);
    }

    public List<Payment> getAllPayments() {
        log.info("Obteniendo todos los pagos");
        List<Payment> payments = paymentRepository.findAll();
        log.info("Se encontraron {} pagos", payments.size());
        return payments;
    }

    public Payment getPaymentById(String id) {
        log.info("Buscando pago con ID: {}", id);
        return paymentRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Pago no encontrado con ID: {}", id);
                    return new RuntimeException("Pago no encontrado con ID: " + id);
                });
    }

    @Transactional
    public Payment updatePaymentStatus(String id, PaymentStatus newStatus) {
        log.info("Actualizando estado del pago {} a {}", id, newStatus);

        Payment payment = getPaymentById(id);
        PaymentStatus oldStatus = payment.getStatus();

        payment.setStatus(newStatus);

        if (PaymentStatus.ACCEPTED.equals(newStatus) && !PaymentStatus.ACCEPTED.equals(oldStatus)) {
            LocalDateTime now = LocalDateTime.now();
            payment.setValidFrom(now);
            payment.setValidUntil(now.plusDays(payment.getPlan().getDurationDays()));

            User user = payment.getUser();
            user.setPlan(payment.getPlan().getPlanName());
            userService.save(user);

            log.info("Usuario {} actualizado con plan {}", user.getId(), user.getPlan());
        }

        if ((PaymentStatus.REJECTED.equals(newStatus) || PaymentStatus.CANCELLED.equals(newStatus)) 
                && PaymentStatus.ACCEPTED.equals(oldStatus)) {
            User user = payment.getUser();
            user.setPlan(null);
            userService.save(user);

            payment.setValidFrom(null);
            payment.setValidUntil(null);

            log.info("Plan removido del usuario {}", user.getId());
        }

        Payment updated = paymentRepository.save(payment);
        log.info("Estado del pago actualizado de {} a {}", oldStatus, newStatus);
        return updated;
    }

    @Transactional
    public void deletePayment(String id) {
        log.info("Eliminando pago con ID: {}", id);

        Payment payment = getPaymentById(id);

        if (PaymentStatus.ACCEPTED.equals(payment.getStatus())) {
            User user = payment.getUser();
            user.setPlan(null);
            userService.save(user);
            log.info("Plan removido del usuario {} antes de eliminar el pago", user.getId());
        }

        paymentRepository.deleteById(id);
        log.info("Pago eliminado exitosamente");
    }

    public Map<String, Object> getPaymentStatistics() {
        log.info("Calculando estadísticas de pagos");

        List<Payment> allPayments = paymentRepository.findAll();
        LocalDateTime now = LocalDateTime.now();

        long totalPayments = allPayments.size();
        long acceptedPayments = allPayments.stream()
                .filter(p -> PaymentStatus.ACCEPTED.equals(p.getStatus()))
                .count();
        long pendingPayments = allPayments.stream()
                .filter(p -> PaymentStatus.PENDING.equals(p.getStatus()))
                .count();
        long rejectedPayments = allPayments.stream()
                .filter(p -> PaymentStatus.REJECTED.equals(p.getStatus()))
                .count();
        long activePayments = allPayments.stream()
                .filter(Payment::isActive)
                .count();

        double totalRevenue = allPayments.stream()
                .filter(p -> PaymentStatus.ACCEPTED.equals(p.getStatus()))
                .mapToDouble(Payment::getAmount)
                .sum();

        LocalDateTime sixMonthsAgo = now.minusMonths(6);
        Map<String, Long> paymentsByMonth = allPayments.stream()
                .filter(p -> p.getCreatedAt() != null && p.getCreatedAt().isAfter(sixMonthsAgo))
                .collect(java.util.stream.Collectors.groupingBy(
                        p -> p.getCreatedAt().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM")),
                        java.util.stream.Collectors.counting()));

        Map<String, Object> stats = new java.util.HashMap<>();
        stats.put("totalPayments", totalPayments);
        stats.put("acceptedPayments", acceptedPayments);
        stats.put("pendingPayments", pendingPayments);
        stats.put("rejectedPayments", rejectedPayments);
        stats.put("activePayments", activePayments);
        stats.put("totalRevenue", totalRevenue);
        stats.put("paymentsByMonth", paymentsByMonth);

        log.info("Estadísticas calculadas: {} pagos totales, {} activos", totalPayments, activePayments);
        return stats;
    }

    public List<Payment> getExpiredPayments() {
        log.info("Buscando pagos expirados");
        LocalDateTime now = LocalDateTime.now();
        return paymentRepository.findExpiredPayments(now);
    }

    public Page<Payment> getPaymentsPaginated(int page, int size, PaymentStatus status, String search) {
        log.info("Obteniendo pagos paginados - Página: {}, Tamaño: {}, Status: {}, Búsqueda: {}",
                page, size, status, search);

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<Payment> paymentsPage;

        if (search != null && !search.trim().isEmpty() && status != null) {
            log.info("Buscando pagos con estado '{}' y término '{}'", status, search);
            paymentsPage = paymentRepository.searchPaymentsByStatus(status, search, pageable);
        } else if (search != null && !search.trim().isEmpty()) {
            log.info("Buscando pagos con término '{}'", search);
            paymentsPage = paymentRepository.searchPayments(search, pageable);
        } else if (status != null) {
            log.info("Filtrando pagos por estado '{}'", status);
            paymentsPage = paymentRepository.findByStatus(status, pageable);
        } else {
            log.info("Obteniendo todos los pagos");
            paymentsPage = paymentRepository.findAll(pageable);
        }

        log.info("Se encontraron {} pagos de {} totales",
                paymentsPage.getNumberOfElements(),
                paymentsPage.getTotalElements());

        return paymentsPage;
    }
}
