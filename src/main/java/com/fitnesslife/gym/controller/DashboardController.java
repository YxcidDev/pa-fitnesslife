package com.fitnesslife.gym.controller;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import com.fitnesslife.gym.enums.Role;
import com.fitnesslife.gym.model.FunctionalTraining;
import com.fitnesslife.gym.model.User;
import com.fitnesslife.gym.service.DashboardService;
import com.fitnesslife.gym.service.FunctionalTrainingService;
import com.fitnesslife.gym.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.fitnesslife.gym.repository.UserRepository;

@Slf4j
@RequiredArgsConstructor
@Controller
public class DashboardController {

    private final FunctionalTrainingService service;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final DashboardService dashboardService;
    private final UserRepository userRepository;

    private static final String VIEW_DASHBOARD = "admin/dashboard";
    private static final String VIEW_USER = "admin/userTable";
    private static final String VIEW_FUNCTIONALTRAINING = "admin/functionalTrainingCrud";

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        log.info("Cargando dashboard");
        model.addAttribute("currentPage", "dashboard");
        return VIEW_DASHBOARD;
    }

    @GetMapping("/api/dashboard/metrics")
    @ResponseBody
    public Map<String, Object> getDashboardMetrics() {
        return dashboardService.getMainMetrics();
    }

    @GetMapping("/api/dashboard/monthly-revenue")
    @ResponseBody
    public List<Map<String, Object>> getMonthlyRevenue() {
        return dashboardService.getMonthlyRevenue();
    }

    @GetMapping("/api/dashboard/recent-transactions")
    @ResponseBody
    public List<Map<String, Object>> getRecentTransactions() {
        return dashboardService.getRecentTransactions();
    }

    @GetMapping("/api/dashboard/plan-sales")
    @ResponseBody
    public List<Map<String, Object>> getPlanSales() {
        return dashboardService.getPlanSalesDistribution();
    }

    @GetMapping("/api/dashboard/access-by-day")
    @ResponseBody
    public List<Map<String, Object>> getAccessByDay() {
        return dashboardService.getAccessByDayOfWeek();
    }

    @GetMapping("/api/dashboard/gender-distribution")
    @ResponseBody
    public List<Map<String, Object>> getGenderDistribution() {
        return dashboardService.getGenderDistribution();
    }

    @GetMapping("/api/dashboard/popular-classes")
    @ResponseBody
    public List<Map<String, Object>> getPopularClasses() {
        return dashboardService.getPopularClasses();
    }

    @GetMapping("/api/dashboard/expiring-plans")
    @ResponseBody
    public List<Map<String, Object>> getExpiringPlans() {
        return dashboardService.getExpiringPlans();
    }

    @GetMapping("/api/dashboard/peak-hours-heatmap")
    @ResponseBody
    public List<Map<String, Object>> getPeakHoursHeatmap() {
        return dashboardService.getPeakHoursHeatmap();
    }

    @GetMapping("/admin/userTable")
    public String userTable(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search,
            Model model) {

        model.addAttribute("currentPage", "usuarios");

        Page<User> usersPage = userService.getUsersPaginated(page, size, role, status, search);

        model.addAttribute("users", usersPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", usersPage.getTotalPages());
        model.addAttribute("totalItems", usersPage.getTotalElements());
        model.addAttribute("pageSize", size);
        model.addAttribute("roleFilter", role != null ? role : "");
        model.addAttribute("statusFilter", status != null ? status : "");
        model.addAttribute("searchTerm", search != null ? search : "");

        model.addAttribute("roles", Role.values());
        model.addAttribute("newUser", new User());

        return VIEW_USER;
    }

    @GetMapping("/admin/functionalTraining")
    public String functionalTraining(
            @RequestParam(required = false, defaultValue = "hoy") String filtro,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Model model) {

        List<FunctionalTraining> trainings = service.getAllTrainings();
        LocalDate hoy = LocalDate.now(ZoneId.systemDefault());
        List<User> trainers = userRepository.findByRole(Role.TRAINER);

        List<FunctionalTraining> clasesDeHoy = trainings.stream()
                .filter(t -> t.getDatetime() != null)
                .filter(t -> {
                    LocalDate fechaClase = t.getDatetime().toInstant()
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate();
                    return fechaClase.isEqual(hoy);
                })
                .sorted(Comparator.comparing(FunctionalTraining::getDatetime))
                .toList();

        List<FunctionalTraining> clasesFiltradas;

        if ("todas".equals(filtro)) {
            clasesFiltradas = trainings.stream()
                    .filter(t -> t.getDatetime() != null)
                    .sorted(Comparator.comparing(FunctionalTraining::getDatetime))
                    .toList();
        } else if ("hoy".equals(filtro)) {
            clasesFiltradas = clasesDeHoy;
        } else if ("semana".equals(filtro)) {
            LocalDate finSemana = hoy.plusDays(7);
            clasesFiltradas = trainings.stream()
                    .filter(t -> t.getDatetime() != null)
                    .filter(t -> {
                        LocalDate fechaClase = t.getDatetime().toInstant()
                                .atZone(ZoneId.systemDefault())
                                .toLocalDate();
                        return !fechaClase.isBefore(hoy) && !fechaClase.isAfter(finSemana);
                    })
                    .sorted(Comparator.comparing(FunctionalTraining::getDatetime))
                    .toList();
        } else {
            LocalDate finMes = hoy.plusMonths(1);
            clasesFiltradas = trainings.stream()
                    .filter(t -> t.getDatetime() != null)
                    .filter(t -> {
                        LocalDate fechaClase = t.getDatetime().toInstant()
                                .atZone(ZoneId.systemDefault())
                                .toLocalDate();
                        return !fechaClase.isBefore(hoy) && !fechaClase.isAfter(finMes);
                    })
                    .sorted(Comparator.comparing(FunctionalTraining::getDatetime))
                    .toList();
        }

        int total = clasesFiltradas.size();
        int totalPages = (int) Math.ceil((double) total / size);
        int start = page * size;
        int end = Math.min(start + size, total);

        List<FunctionalTraining> clasesPaginadas = (start < total)
                ? clasesFiltradas.subList(start, end)
                : Collections.emptyList();

        model.addAttribute("currentPage", "clases");
        model.addAttribute("clases", clasesDeHoy);
        model.addAttribute("trainings", clasesPaginadas);
        model.addAttribute("training", new FunctionalTraining());
        model.addAttribute("filtro", filtro);
        model.addAttribute("page", page);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("total", total);
        model.addAttribute("trainers", trainers);

        return VIEW_FUNCTIONALTRAINING;
    }

    @PostMapping("/admin/users/create")
    public String createUser(
            @ModelAttribute User user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) String roleFilter,
            @RequestParam(required = false) String statusFilter,
            @RequestParam(required = false) String search,
            RedirectAttributes redirectAttributes) {
        try {
            if (user.getEmail() == null || user.getEmail().trim().isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "El email es requerido");
                return buildRedirectUrl("/admin/userTable", page, roleFilter, statusFilter, search);
            }

            if (user.getPassword() != null && !user.getPassword().trim().isEmpty()) {
                user.setPassword(passwordEncoder.encode(user.getPassword()));
            } else {
                redirectAttributes.addFlashAttribute("error", "La contraseña es requerida");
                return buildRedirectUrl("/admin/userTable", page, roleFilter, statusFilter, search);
            }

            if (user.getRole() == null) {
                user.setRole(Role.USER);
            }
            user.setActive(true);

            userService.createUser(user);
            redirectAttributes.addFlashAttribute("success", "Usuario creado exitosamente");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al crear usuario: " + e.getMessage());
        }

        return buildRedirectUrl("/admin/userTable", page, roleFilter, statusFilter, search);
    }

    @PostMapping("/admin/users/update/{id}")
    public String updateUser(
            @PathVariable String id,
            @ModelAttribute User user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) String roleFilter,
            @RequestParam(required = false) String statusFilter,
            @RequestParam(required = false) String search,
            RedirectAttributes redirectAttributes) {
        try {
            userService.updateUser(id, user);
            redirectAttributes.addFlashAttribute("success", "Usuario actualizado exitosamente");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al actualizar usuario: " + e.getMessage());
        }

        return buildRedirectUrl("/admin/userTable", page, roleFilter, statusFilter, search);
    }

    @PostMapping("/update-role")
    public String updateUserRole(
            @RequestParam String id,
            @RequestParam String role,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) String roleFilter,
            @RequestParam(required = false) String statusFilter,
            @RequestParam(required = false) String search,
            RedirectAttributes redirectAttributes) {
        try {
            Role roleEnum = Role.valueOf(role.toUpperCase());
            userService.updateUserRole(id, roleEnum);
            redirectAttributes.addFlashAttribute("success", "Rol actualizado correctamente");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al actualizar el rol");
        }

        return buildRedirectUrl("/admin/userTable", page, roleFilter, statusFilter, search);
    }

    @PostMapping("/admin/users/delete/{id}")
    public String deleteUser(
            @PathVariable("id") String id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) String roleFilter,
            @RequestParam(required = false) String statusFilter,
            @RequestParam(required = false) String search,
            RedirectAttributes redirectAttributes,
            @AuthenticationPrincipal UserDetails currentUser) {
        try {
            Optional<User> userOpt = userService.findById(id);

            if (userOpt.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Usuario no encontrado");
                return buildRedirectUrl("/admin/userTable", page, roleFilter, statusFilter, search);
            }

            User user = userOpt.get();

            if (user.getEmail().equals(currentUser.getUsername())) {
                redirectAttributes.addFlashAttribute("error", "No puedes eliminarte a ti mismo");
                return buildRedirectUrl("/admin/userTable", page, roleFilter, statusFilter, search);
            }

            userService.deleteById(id);

            redirectAttributes.addFlashAttribute("success", "Usuario eliminado exitosamente");

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al eliminar el usuario: " + e.getMessage());
        }

        return buildRedirectUrl("/admin/userTable", page, roleFilter, statusFilter, search);
    }

    @GetMapping("/admin/users/get/{id}")
    @ResponseBody
    public User getUserById(@PathVariable String id) {
        return userService.getUserByIdOrThrow(id);
    }

    private String buildRedirectUrl(String basePath, int page, String roleFilter, String statusFilter, String search) {
        StringBuilder url = new StringBuilder("redirect:");
        url.append(basePath);
        url.append("?page=").append(page);

        if (roleFilter != null && !roleFilter.trim().isEmpty()) {
            url.append("&role=").append(roleFilter);
        }

        if (statusFilter != null && !statusFilter.trim().isEmpty()) {
            url.append("&status=").append(statusFilter);
        }

        if (search != null && !search.trim().isEmpty()) {
            url.append("&search=").append(search);
        }

        return url.toString();
    }
}
