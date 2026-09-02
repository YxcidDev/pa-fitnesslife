package com.fitnesslife.gym.controller;

import java.security.Principal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import com.fitnesslife.gym.model.FunctionalTraining;
import com.fitnesslife.gym.model.User;
import com.fitnesslife.gym.repository.UserRepository;
import com.fitnesslife.gym.service.FunctionalTrainingService;
import com.fitnesslife.gym.service.PaymentService;
import com.fitnesslife.gym.service.PlanService;
import com.fitnesslife.gym.service.UserService;
import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class PageController {

    private final UserService userService;
    private final UserRepository userRepository;
    private final FunctionalTrainingService service;
    private final PlanService planService;
    private final PaymentService paymentService;

    private static final String VIEW_INDEX = "index";
    private static final String VIEW_LOGIN = "auth/login";
    private static final String VIEW_HOME = "client/home";
    private static final String VIEW_QR_CODE = "client/qr-code";
    private static final String VIEW_USER_PROFILE = "client/user-profile";
    private static final String VIEW_TRAINER = "trainer/trainer-profile";

    @ModelAttribute
    public void addUserToModel(Model model, Principal principal) {
        if (principal != null) {
            String email = principal.getName();
            userService.findByEmail(email).ifPresentOrElse(
                    user -> model.addAttribute("currentUser", user),
                    () -> model.addAttribute("error", "Usuario no encontrado"));
        }
    }

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("availablePlans", planService.getAllPlans());
        return VIEW_INDEX;
    }

    @GetMapping("/login")
    public String login() {
        return VIEW_LOGIN;
    }

    @GetMapping("/home")
    public String home(Model model, Principal principal) {

        service.actualizarEstados();
        LocalDate hoy = LocalDate.now(ZoneId.systemDefault());

        String email = principal.getName();
        User user = userRepository.findByEmail(email).orElse(null);

        if (user != null) {
            model.addAttribute("identification", user.getIdentification());

            boolean hasActivePlan = paymentService.getActivePayment(user.getId()).isPresent();
            model.addAttribute("hasActivePlan", hasActivePlan);

            List<FunctionalTraining> misClases = service.getTrainingsByUser(email);
            List<FunctionalTraining> misClasesHoy = misClases.stream()
                    .filter(t -> {
                        LocalDate fechaClase = t.getDatetime()
                                .toInstant()
                                .atZone(ZoneId.systemDefault())
                                .toLocalDate();
                        return fechaClase.isEqual(hoy);
                    })
                    .toList();
            model.addAttribute("confirmedClasses", misClasesHoy);

        } else {
            model.addAttribute("identification", null);
            model.addAttribute("hasActivePlan", false);
        }

        List<FunctionalTraining> todasLasClases = service.getAllTrainings();

        List<FunctionalTraining> clasesDeHoy = todasLasClases.stream()
                .filter(t -> t.getDatetime() != null)
                .filter(t -> {
                    LocalDate fechaClase = t.getDatetime().toInstant()
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate();
                    return fechaClase.isEqual(hoy);
                })
                .toList();

        model.addAttribute("trainings", clasesDeHoy);

        return VIEW_HOME;
    }

    @GetMapping("/qr-code")
    public String qrCode() {
        return VIEW_QR_CODE;
    }

    @GetMapping("/user-profile")
    public String userProfile() {
        return VIEW_USER_PROFILE;
    }

    @GetMapping("/trainer-profile")
    public String trainerProfile(Principal principal, Model model) {

        service.actualizarEstados();

        User trainer = userRepository.findByEmail(principal.getName()).orElse(null);

        String nombreCompleto = trainer.getName() + " " + trainer.getLastname();
        ZonedDateTime ahora = ZonedDateTime.now(ZoneId.systemDefault());
        LocalDate hoy = ahora.toLocalDate();

        List<FunctionalTraining> todas = service.getAllTrainings().stream()
                .filter(c -> c.getDatetime() != null)
                .filter(c -> c.getInstructor().equalsIgnoreCase(nombreCompleto))
                .toList();

        List<FunctionalTraining> clasesHoy = todas.stream()
                .filter(c -> c.getDatetime().toInstant().atZone(ZoneId.systemDefault()).toLocalDate().isEqual(hoy))
                .toList();

        List<FunctionalTraining> historial = todas.stream()
                .filter(c -> c.getDatetime().toInstant().atZone(ZoneId.systemDefault()).toLocalDate().isBefore(hoy))
                .sorted(Comparator.comparing(FunctionalTraining::getDatetime).reversed())
                .toList();

        List<FunctionalTraining> proximasClases = todas.stream()
                .filter(c -> c.getDatetime().toInstant().atZone(ZoneId.systemDefault()).isAfter(ahora))
                .sorted(Comparator.comparing(c -> c.getDatetime().toInstant().atZone(ZoneId.systemDefault())))
                .toList();

        model.addAttribute("trainer", trainer);
        model.addAttribute("clasesHoy", clasesHoy);
        model.addAttribute("historialClases", historial);
        model.addAttribute("proximasClases", proximasClases);

        return VIEW_TRAINER;
    }

}
