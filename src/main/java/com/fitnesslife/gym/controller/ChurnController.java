package com.fitnesslife.gym.controller;

import com.fitnesslife.gym.model.ChurnPrediction;
import com.fitnesslife.gym.repository.ChurnPredictionRepository;
import com.fitnesslife.gym.service.ChurnPredictionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;
import java.util.List;
import java.util.Map;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ChurnController {

    private final ChurnPredictionService churnPredictionService;
    private final ChurnPredictionRepository churnPredictionRepository;

    private static final int PAGE_SIZE = 20;

    @GetMapping("/admin/churn")
    public String churnDashboard(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) String risk,
            @RequestParam(required = false) String profile,
            @RequestParam(required = false) String search,
            Model model,
            Principal principal) {

        log.debug("[ChurnController] Dashboard churn — página={}, risk={}, profile={}, search={}",
                page, risk, profile, search);

        Pageable pageable = PageRequest.of(page, PAGE_SIZE, Sort.by("riskOrder").ascending());

        Page<ChurnPrediction> predictionsPage = resolvePage(risk, profile, search, pageable);

        long highCount = churnPredictionRepository.countByRiskLevel("HIGH");
        long mediumCount = churnPredictionRepository.countByRiskLevel("MEDIUM");
        long lowCount = churnPredictionRepository.countByRiskLevel("LOW");
        long total = highCount + mediumCount + lowCount;

        double churnPct = total > 0
                ? Math.round(((double) (highCount + mediumCount) / total) * 1000.0) / 10.0
                : 0.0;

        boolean hasData = total > 0;

        java.time.LocalDateTime lastCalculated = predictionsPage.getContent().stream()
                .map(ChurnPrediction::getCalculatedAt)
                .filter(java.util.Objects::nonNull)
                .max(java.util.Comparator.naturalOrder())
                .orElse(null);

        model.addAttribute("churnResults", predictionsPage.getContent());
        model.addAttribute("highCount", highCount);
        model.addAttribute("mediumCount", mediumCount);
        model.addAttribute("lowCount", lowCount);
        model.addAttribute("totalUsers", total);
        model.addAttribute("churnPct", churnPct);
        model.addAttribute("hasData", hasData);
        model.addAttribute("lastCalculated", lastCalculated);
        model.addAttribute("currentPage", predictionsPage.getNumber());
        model.addAttribute("totalPages", predictionsPage.getTotalPages());
        model.addAttribute("totalItems", predictionsPage.getTotalElements());
        model.addAttribute("pageSize", PAGE_SIZE);
        model.addAttribute("riskFilter", risk != null ? risk : "");
        model.addAttribute("profileFilter", profile != null ? profile : "");
        model.addAttribute("searchTerm", search != null ? search : "");
        model.addAttribute("currentPageNav", "churn");

        return "admin/churn-management";
    }

    private Page<ChurnPrediction> resolvePage(
            String risk, String profile, String search, Pageable pageable) {

        boolean hasRisk = risk != null && !risk.trim().isEmpty();
        boolean hasProfile = profile != null && !profile.trim().isEmpty();
        boolean hasSearch = search != null && !search.trim().isEmpty();

        if (hasSearch && hasRisk && hasProfile) {
            return churnPredictionRepository
                    .searchByNameAndRiskAndProfile(risk, profile, search, pageable);
        }
        if (hasSearch && hasRisk) {
            return churnPredictionRepository.searchByNameAndRisk(risk, search, pageable);
        }
        if (hasSearch && hasProfile) {
            return churnPredictionRepository.searchByNameAndProfile(profile, search, pageable);
        }
        if (hasSearch) {
            return churnPredictionRepository.searchByName(search, pageable);
        }
        if (hasRisk && hasProfile) {
            return churnPredictionRepository.findByRiskLevelAndProfile(risk, profile, pageable);
        }
        if (hasRisk) {
            return churnPredictionRepository.findByRiskLevel(risk, pageable);
        }
        if (hasProfile) {
            return churnPredictionRepository.findByProfile(profile, pageable);
        }
        return churnPredictionRepository.findAll(pageable);
    }

    @GetMapping("/api/churn/users")
    @ResponseBody
    public ResponseEntity<List<ChurnPrediction>> getChurnUsers(
            @RequestParam(required = false) String risk) {

        List<ChurnPrediction> results = risk != null && !risk.isBlank()
                ? churnPredictionRepository.findByRiskLevel(risk.toUpperCase())
                : churnPredictionRepository.findAll();

        return ResponseEntity.ok(results);
    }

    @PostMapping("/api/churn/recalculate")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> forceRecalculate() {
        log.info("[ChurnController] Recálculo manual disparado");
        long start = System.currentTimeMillis();
        var results = churnPredictionService.recalculateAndPersist();
        long elapsed = System.currentTimeMillis() - start;

        return ResponseEntity.ok(Map.of(
                "status", "ok",
                "processed", results.size(),
                "elapsedMs", elapsed));
    }
}