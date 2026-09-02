package com.fitnesslife.gym.controller;

import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.fitnesslife.gym.model.Plan;
import com.fitnesslife.gym.service.PlanService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Slf4j
@Controller
@RequestMapping("/admin/plans")
@RequiredArgsConstructor
public class AdminPlanController {

    private final PlanService planService;

    @GetMapping
    public String showPlansManagement(
            @RequestParam(defaultValue = "0") int page,
            Model model) {
        try {
            log.info("Cargando gestión de planes - Página: {}", page);

            Pageable pageable = PageRequest.of(page, 10, Sort.by("price").ascending());
            Page<Plan> plansPage = planService.getAllPlansWithPagination(pageable);

            model.addAttribute("plans", plansPage.getContent());
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", plansPage.getTotalPages());
            model.addAttribute("totalItems", plansPage.getTotalElements());
            model.addAttribute("newPlan", new Plan());

            log.info("Se cargaron {} planes de un total de {}",
                    plansPage.getContent().size(), plansPage.getTotalElements());

            return "admin/plans";
        } catch (Exception e) {
            log.error("Error al cargar planes: {}", e.getMessage(), e);
            model.addAttribute("error", "Error al cargar los planes");
            return "admin/plans";
        }
    }

    @PostMapping("/create")
    public String createPlan(@ModelAttribute Plan plan, RedirectAttributes redirectAttributes) {
        try {
            log.info("Creando nuevo plan: {}", plan.getPlanName());
            planService.createPlan(plan);
            redirectAttributes.addFlashAttribute("success", "Plan creado exitosamente");
            log.info("Plan creado con éxito: {}", plan.getPlanName());
        } catch (Exception e) {
            log.error("Error al crear plan: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "Error al crear el plan: " + e.getMessage());
        }
        return "redirect:/admin/plans";
    }

    @PostMapping("/update/{id}")
    public String updatePlan(
            @PathVariable String id,
            @ModelAttribute Plan plan,
            RedirectAttributes redirectAttributes) {
        try {
            log.info("Actualizando plan con ID: {}", id);
            plan.setId(id);
            planService.updatePlan(plan);
            redirectAttributes.addFlashAttribute("success", "Plan actualizado exitosamente");
            log.info("Plan actualizado con éxito: {}", plan.getPlanName());
        } catch (Exception e) {
            log.error("Error al actualizar plan: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "Error al actualizar el plan: " + e.getMessage());
        }
        return "redirect:/admin/plans";
    }

    @PostMapping("/delete/{id}")
    public String deletePlan(@PathVariable String id, RedirectAttributes redirectAttributes) {
        try {
            log.info("Eliminando plan con ID: {}", id);
            planService.deletePlan(id);
            redirectAttributes.addFlashAttribute("success", "Plan eliminado exitosamente");
            log.info("Plan eliminado con éxito");
        } catch (Exception e) {
            log.error("Error al eliminar plan: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "Error al eliminar el plan: " + e.getMessage());
        }
        return "redirect:/admin/plans";
    }

    @GetMapping("/get/{id}")
    @ResponseBody
    public Plan getPlanById(@PathVariable String id) {
        log.info("Obteniendo plan con ID: {}", id);
        return planService.getPlanById(id);
    }
}
