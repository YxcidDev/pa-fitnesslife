package com.fitnesslife.gym.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.fitnesslife.gym.model.Plan;
import com.fitnesslife.gym.repository.PlanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlanService {

    private final PlanRepository planRepository;

    public List<Plan> getAllPlans() {
        log.info("Obteniendo todos los planes disponibles");
        List<Plan> plans = planRepository.findAllByOrderByPriceAsc();
        log.info("Se encontraron {} planes", plans.size());
        return plans;
    }

    public Plan getPlanById(String id) {
        log.info("Buscando plan con ID: {}", id);
        return planRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Plan no encontrado con ID: {}", id);
                    return new RuntimeException("Plan no encontrado con ID: " + id);
                });
    }

    public Plan createPlan(Plan plan) {
        log.info("Creando nuevo plan: {}", plan.getPlanName());

        if (plan.getCreatedAt() == null) {
            plan.setCreatedAt(LocalDateTime.now());
        }
        plan.setUpdatedAt(LocalDateTime.now());

        Plan savedPlan = planRepository.save(plan);
        log.info("Plan creado exitosamente con ID: {}", savedPlan.getId());
        return savedPlan;
    }

    public Plan updatePlan(Plan plan) {
        log.info("Actualizando plan con ID: {}", plan.getId());

        Plan existingPlan = getPlanById(plan.getId());

        existingPlan.setPlanName(plan.getPlanName());
        existingPlan.setPrice(plan.getPrice());
        existingPlan.setCurrency(plan.getCurrency());
        existingPlan.setDurationDays(plan.getDurationDays());
        existingPlan.setBadge(plan.getBadge());
        existingPlan.setBenefits(plan.getBenefits());
        existingPlan.setUpdatedAt(LocalDateTime.now());

        Plan updatedPlan = planRepository.save(existingPlan);
        log.info("Plan actualizado exitosamente: {}", updatedPlan.getPlanName());
        return updatedPlan;
    }

    public void deletePlan(String id) {
        log.info("Eliminando plan con ID: {}", id);

        Plan plan = getPlanById(id);

        planRepository.deleteById(id);
        log.info("Plan eliminado exitosamente: {}", plan.getPlanName());
    }

    public Page<Plan> getAllPlansWithPagination(Pageable pageable) {
        return planRepository.findAllByOrderByPriceAsc(pageable);
    }
}
