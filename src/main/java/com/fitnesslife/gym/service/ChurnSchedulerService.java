package com.fitnesslife.gym.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChurnSchedulerService {

    private final ChurnPredictionService churnPredictionService;

    @Scheduled(cron = "0 0 */6 * * *")
    public void recalculateChurnPredictions() {
        log.info("[ChurnScheduler] Disparando recálculo programado de predicciones...");
        try {
            var results = churnPredictionService.recalculateAndPersist();
            long high   = results.stream().filter(r -> "HIGH".equals(r.getRiskLevel())).count();
            long medium = results.stream().filter(r -> "MEDIUM".equals(r.getRiskLevel())).count();
            long low    = results.stream().filter(r -> "LOW".equals(r.getRiskLevel())).count();
            log.info("[ChurnScheduler] Completado — HIGH: {}, MEDIUM: {}, LOW: {}", high, medium, low);
        } catch (Exception e) {
            log.error("[ChurnScheduler] Error durante el recálculo: {}", e.getMessage(), e);
        }
    }
}