package com.fitnesslife.gym.service;

import com.fitnesslife.gym.enums.ClassesStatus;
import com.fitnesslife.gym.model.FunctionalTraining;
import com.fitnesslife.gym.model.User;
import com.fitnesslife.gym.repository.FunctionalTrainingRepository;
import com.fitnesslife.gym.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@Service
@Transactional(readOnly = true)
public class FunctionalTrainingService {

    private final FunctionalTrainingRepository repository;
    private final UserRepository userRepository;

    public List<FunctionalTraining> getAllTrainings() {
        log.info("Obteniendo todas las clases funcionales");
        return repository.findAll();
    }

    public Optional<FunctionalTraining> getTrainingById(String id) {
        log.info("Buscando clase con ID: {}", id);
        return repository.findById(id);
    }

    public Optional<FunctionalTraining> getTrainingByFunctionalId(int idFunctionalTraining) {
        log.info("Buscando clase con ID funcional: {}", idFunctionalTraining);
        return repository.findByIdFunctionalTraining(idFunctionalTraining);
    }

    @Transactional
    public FunctionalTraining createTraining(FunctionalTraining training) {
        log.info("Creando nueva clase funcional: {}", training.getNameTraining());

        long count = repository.count();
        training.setIdFunctionalTraining((int) count + 1);
        training.setStatus(ClassesStatus.ACTIVE);

        FunctionalTraining saved = repository.save(training);
        log.info("Clase funcional creada con ID: {}", saved.getId());
        return saved;
    }

    @Transactional
    public FunctionalTraining updateTraining(String id, FunctionalTraining updatedTraining) {
        log.info("Actualizando clase con ID: {}", id);

        return repository.findById(id).map(training -> {
            training.setNameTraining(updatedTraining.getNameTraining());
            training.setInstructor(updatedTraining.getInstructor());
            training.setDescription(updatedTraining.getDescription());
            training.setMaximumCapacity(updatedTraining.getMaximumCapacity());
            training.setDuration(updatedTraining.getDuration());
            training.setStatus(updatedTraining.getStatus());
            training.setDatetime(updatedTraining.getDatetime());
            training.setRoom(updatedTraining.getRoom());

            FunctionalTraining saved = repository.save(training);
            log.info("Clase actualizada exitosamente: {}", saved.getNameTraining());
            return saved;
        }).orElseThrow(() -> new IllegalArgumentException("Training not found"));
    }

    @Transactional
    public void deleteTraining(String id) {
        log.info("Eliminando clase con ID: {}", id);
        repository.deleteById(id);
        log.info("Clase eliminada exitosamente");
    }

    @Transactional
    public void cancelarInscripcion(int idFunctionalTraining, String emailUsuario) {
        log.info("Cancelando inscripción del usuario {} en clase {}", emailUsuario, idFunctionalTraining);

        User user = userRepository.findByEmail(emailUsuario)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        FunctionalTraining training = repository.findByIdFunctionalTraining(idFunctionalTraining)
                .orElseThrow(() -> new RuntimeException("Clase no encontrada"));

        if (training.getUserIds() != null && training.getUserIds().contains(user.getIdentification())) {
            training.getUserIds().remove(user.getIdentification());
            repository.save(training);
            log.info("Inscripción cancelada exitosamente");
        }
    }

    public FunctionalTraining findByIdFunctional(int idFunctional) {
        log.info("Buscando clase funcional con ID: {}", idFunctional);
        return repository.findByIdFunctionalTraining(idFunctional).orElse(null);
    }

    public List<FunctionalTraining> getTrainingsByUser(String emailUsuario) {
        log.info("Obteniendo clases del usuario: {}", emailUsuario);

        User user = userRepository.findByEmail(emailUsuario)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        List<FunctionalTraining> allTrainings = repository.findAll();

        return allTrainings.stream()
                .filter(training -> training.getUserIds() != null &&
                        training.getUserIds().contains(user.getIdentification()))
                .toList();
    }

    @Transactional
    public void actualizarEstados() {
        List<FunctionalTraining> clases = repository.findAll();
        LocalDateTime ahora = LocalDateTime.now();

        clases.forEach(c -> {
            LocalDateTime fechaClase = c.getDatetime()
                    .toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDateTime();

            if (fechaClase.isBefore(ahora) && c.getStatus() != ClassesStatus.INACTIVE) {
                c.setStatus(ClassesStatus.INACTIVE);
                repository.save(c);
            }
        });
    }
}
