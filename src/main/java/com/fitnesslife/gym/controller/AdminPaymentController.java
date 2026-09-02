package com.fitnesslife.gym.controller;

import com.fitnesslife.gym.enums.PaymentStatus;
import com.fitnesslife.gym.model.Payment;
import com.fitnesslife.gym.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.util.Map;

@Controller
@RequestMapping("/admin/payments")
@RequiredArgsConstructor
@Slf4j
public class AdminPaymentController {

    private final PaymentService paymentService;

    @GetMapping
    public String showPaymentsManagement(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search,
            Model model) {
        try {
            log.info("Cargando gestión de pagos - Página: {}, Tamaño: {}, Status: {}, Búsqueda: {}",
                    page, size, status, search);
            PaymentStatus paymentStatus = null;
            if (status != null && !status.trim().isEmpty()) {
                try {
                    paymentStatus = PaymentStatus.valueOf(status.toUpperCase());
                } catch (IllegalArgumentException e) {
                    log.warn("Estado de pago inválido recibido: {}", status);
                }
            }

            Page<Payment> paymentsPage = paymentService.getPaymentsPaginated(page, size, paymentStatus, search);

            Map<String, Object> statistics = paymentService.getPaymentStatistics();

            model.addAttribute("payments", paymentsPage.getContent());
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", paymentsPage.getTotalPages());
            model.addAttribute("totalItems", paymentsPage.getTotalElements());
            model.addAttribute("pageSize", size);
            model.addAttribute("statusFilter", status != null ? status : "");
            model.addAttribute("searchTerm", search != null ? search : "");

            model.addAttribute("totalPayments", statistics.get("totalPayments"));
            model.addAttribute("acceptedPayments", statistics.get("acceptedPayments"));
            model.addAttribute("pendingPayments", statistics.get("pendingPayments"));
            model.addAttribute("rejectedPayments", statistics.get("rejectedPayments"));
            model.addAttribute("activePayments", statistics.get("activePayments"));
            model.addAttribute("totalRevenue", statistics.get("totalRevenue"));

            log.info("Se cargaron {} pagos de {} totales",
                    paymentsPage.getContent().size(),
                    paymentsPage.getTotalElements());
            return "admin/payments";
        } catch (Exception e) {
            log.error("Error al cargar pagos: {}", e.getMessage(), e);
            model.addAttribute("error", "Error al cargar los pagos");
            return "admin/payments";
        }
    }

    @GetMapping("/get/{id}")
    @ResponseBody
    public Payment getPaymentById(@PathVariable String id) {
        log.info("Obteniendo pago con ID: {}", id);
        return paymentService.getPaymentById(id);
    }

    @PostMapping("/update-status/{id}")
    public String updatePaymentStatus(
            @PathVariable String id,
            @RequestParam String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) String statusFilter,
            @RequestParam(required = false) String search,
            RedirectAttributes redirectAttributes) {
        try {
            log.info("Actualizando estado del pago {} a {}", id, status);
            PaymentStatus paymentStatus;
            try {
                paymentStatus = PaymentStatus.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException e) {
                log.error("Estado de pago inválido: {}", status);
                redirectAttributes.addFlashAttribute("error", "Estado de pago inválido: " + status);
                return "redirect:/admin/payments?page=" + page +
                        (statusFilter != null ? "&status=" + statusFilter : "") +
                        (search != null ? "&search=" + search : "");
            }
            paymentService.updatePaymentStatus(id, paymentStatus);
            redirectAttributes.addFlashAttribute("success", "Estado del pago actualizado exitosamente");
            log.info("Estado del pago actualizado correctamente");
        } catch (Exception e) {
            log.error("Error al actualizar estado del pago: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "Error al actualizar el estado: " + e.getMessage());
        }

        return "redirect:/admin/payments?page=" + page +
                (statusFilter != null ? "&status=" + statusFilter : "") +
                (search != null ? "&search=" + search : "");
    }

    @PostMapping("/delete/{id}")
    public String deletePayment(
            @PathVariable String id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) String statusFilter,
            @RequestParam(required = false) String search,
            RedirectAttributes redirectAttributes) {
        try {
            log.info("Eliminando pago con ID: {}", id);
            paymentService.deletePayment(id);
            redirectAttributes.addFlashAttribute("success", "Pago eliminado exitosamente");
            log.info("Pago eliminado con éxito");
        } catch (Exception e) {
            log.error("Error al eliminar pago: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "Error al eliminar el pago: " + e.getMessage());
        }

        return "redirect:/admin/payments?page=" + page +
                (statusFilter != null ? "&status=" + statusFilter : "") +
                (search != null ? "&search=" + search : "");
    }
}
