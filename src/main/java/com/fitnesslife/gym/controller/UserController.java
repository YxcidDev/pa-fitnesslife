package com.fitnesslife.gym.controller;

import com.fitnesslife.gym.service.UserService;
import java.security.Principal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import com.fitnesslife.gym.enums.Role;
import com.fitnesslife.gym.model.FunctionalTraining;
import com.fitnesslife.gym.model.User;
import com.fitnesslife.gym.repository.FunctionalTrainingRepository;
import com.fitnesslife.gym.repository.UserRepository;
import com.fitnesslife.gym.service.FunctionalTrainingService;
import com.fitnesslife.gym.service.PaymentService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Slf4j
@Controller
public class UserController {

    private final UserRepository userRepo;
    private final PasswordEncoder encoder;
    private final UserService userService;
    private final FunctionalTrainingRepository trainingRepo;
    private final FunctionalTrainingService functionalTrainingService;
    private final PaymentService paymentService;

    @PostMapping("/register")
    public String register(@ModelAttribute User user, Model model) {

        if (userRepo.findByEmail(user.getEmail()).isPresent()) {
            model.addAttribute("error", "El usuario ya existe");
            return "register";
        }

        user.setPassword(encoder.encode(user.getPassword()));
        user.setRole(Role.USER);
        user.setActive(true);

        user.setSex(null);
        user.setBirthDate(null);
        user.setBloodType(null);
        user.setPhotoProfile("");
        user.setPlan("");
        user.setQrCodePath("");
        user.setLastLogin(null);

        userRepo.save(user);

        return "redirect:/login?registered";
    }

    @PostMapping("/inscribirme/{idFunctionalTraining}")
    public String inscribirme(@PathVariable int idFunctionalTraining, Principal principal) {

        User user = userRepo.findByEmail(principal.getName()).orElseThrow();

        boolean hasActivePlan = paymentService.getActivePayment(user.getId()).isPresent();
        if (!hasActivePlan) {
            return "redirect:/home?error=sin-plan";
        }

        FunctionalTraining training = trainingRepo.findByIdFunctionalTraining(idFunctionalTraining).orElseThrow();

        if (training.getUserIds().contains(user.getIdentification())) {
            return "redirect:/home?error=ya-inscrito";
        }

        training.getUserIds().add(user.getIdentification());
        trainingRepo.save(training);

        return "redirect:/home?success=inscrito";
    }

    @PostMapping("/cancelarInscripcion/{id}")
    public String cancelarInscripcion(@PathVariable int id, Principal principal) {
        functionalTrainingService.cancelarInscripcion(id, principal.getName());
        return "redirect:/home?success=Cancelada";
    }

    @PostMapping("/actualizarUserPerfil")
    public String actualizarPerfil(@ModelAttribute User userForm, Principal principal) {
        User user = userRepo.findByEmail(principal.getName()).orElseThrow();

        user.setName(userForm.getName());
        user.setLastname(userForm.getLastname());
        user.setPhone(userForm.getPhone());
        user.setSex(userForm.getSex());
        user.setBirthDate(userForm.getBirthDate());
        user.setBloodType(userForm.getBloodType());

        userRepo.save(user);
        return "redirect:/user-profile";
    }

    @PostMapping("/update-profile-picture")
    public String updateProfilePicture(@RequestParam("profileImage") MultipartFile profileImage,
            Principal principal) {
        try {
            userService.updateProfilePicture(principal.getName(), profileImage);
            return "redirect:/user-profile?success=uploaded";
        } catch (Exception e) {
            log.error("Error uploading profile picture: {}", e.getMessage());
            return "redirect:/user-profile?error=upload-failed";
        }
    }
}
