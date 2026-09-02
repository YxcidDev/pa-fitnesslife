package com.fitnesslife.gym.controller;

import com.fitnesslife.gym.enums.AccessResult;
import com.fitnesslife.gym.model.Attendance;
import com.fitnesslife.gym.service.AttendanceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/accesses")
@RequiredArgsConstructor
@Slf4j
public class AdminAttendanceController {

    private final AttendanceService attendanceService;

    @GetMapping
    public String showAttendances(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String result,
            @RequestParam(required = false) String search,
            Model model) {

        log.info("Loading attendances — page: {}, result: {}, search: {}", page, result, search);

        AccessResult accessResult = null;
        if (result != null && !result.isBlank()) {
            try {
                accessResult = AccessResult.valueOf(result.toUpperCase());
            } catch (IllegalArgumentException e) {
                log.warn("Invalid result filter: {}", result);
            }
        }

        Page<Attendance> pageData = attendanceService
                .getAttendancesPaginated(page, size, accessResult, search);

        model.addAttribute("accesses", pageData.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", pageData.getTotalPages());
        model.addAttribute("totalItems", pageData.getTotalElements());
        model.addAttribute("pageSize", size);
        model.addAttribute("resultFilter", result != null ? result : "");
        model.addAttribute("searchTerm", search != null ? search : "");

        return "admin/accesses";
    }

    @GetMapping("/get/{id}")
    @ResponseBody
    public Attendance getById(@PathVariable String id) {
        return attendanceService.getAttendanceById(id);
    }

    @PostMapping("/update-result/{id}")
    public String updateResult(
            @PathVariable String id,
            @RequestParam String result,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) String resultFilter,
            @RequestParam(required = false) String search,
            RedirectAttributes ra) {
        try {
            log.info("Actualizando resultado del acceso {} a {}", id, result);
            AccessResult accessResult;
            try {
                accessResult = AccessResult.valueOf(result.toUpperCase());
            } catch (IllegalArgumentException e) {
                log.error("Resultado inválido: {}", result);
                ra.addFlashAttribute("error", "Resultado inválido: " + result);
                return buildRedirect(page, resultFilter, search);
            }
            attendanceService.updateAttendanceResult(id, accessResult);
            ra.addFlashAttribute("success", "Resultado actualizado exitosamente");
            log.info("Resultado del acceso {} actualizado a {}", id, result);
        } catch (Exception e) {
            log.error("Error al actualizar resultado: {}", e.getMessage(), e);
            ra.addFlashAttribute("error", "Error al actualizar: " + e.getMessage());
        }
        return buildRedirect(page, resultFilter, search);
    }

    @PostMapping("/delete/{id}")
    public String delete(
            @PathVariable String id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) String resultFilter,
            @RequestParam(required = false) String search,
            RedirectAttributes ra) {
        try {
            attendanceService.deleteAttendance(id);
            ra.addFlashAttribute("success", "Registro eliminado exitosamente");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Error al eliminar: " + e.getMessage());
        }
        return buildRedirect(page, resultFilter, search);
    }

    private String buildRedirect(int page, String resultFilter, String search) {
        return "redirect:/admin/accesses?page=" + page
                + (resultFilter != null ? "&result=" + resultFilter : "")
                + (search != null ? "&search=" + search : "");
    }
}