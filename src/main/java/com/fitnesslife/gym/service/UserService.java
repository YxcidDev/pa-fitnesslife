package com.fitnesslife.gym.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.nio.file.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import com.fitnesslife.gym.model.User;
import com.fitnesslife.gym.repository.UserRepository;
import com.fitnesslife.gym.enums.Role;
import com.fitnesslife.gym.exception.UserNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final String UPLOAD_DIRECTORY = "uploads/profiles";

    public Optional<User> findByEmail(String email) {
        log.debug("Buscando usuario con email: {}", email);
        return userRepository.findByEmail(email);
    }

    public User getUserOrThrow(String email) {
        return findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("Usuario no encontrado: " + email));
    }

    public Optional<User> findById(String id) {
        log.debug("Buscando usuario con ID: {}", id);
        return userRepository.findById(id);
    }

    public User getUserByIdOrThrow(String id) {
        return findById(id)
                .orElseThrow(() -> new UserNotFoundException("Usuario no encontrado con ID: " + id));
    }

    public Optional<User> getUserByIdentification(Long identification) {
        log.info("Buscando usuario por identificación: {}", identification);
        return userRepository.findByIdentification(identification);
    }

    public Optional<User> getUserByQrCode(String qrCodePath) {
        log.info("Buscando usuario por QR Code: {}", qrCodePath);
        return userRepository.findByQrCodePath(qrCodePath);
    }

    @Transactional
    public User save(User user) {
        log.debug("Guardando usuario: {}", user.getEmail());
        return userRepository.save(user);
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public Page<User> getUsersPaginated(int page, int size, String role, String status, String search) {
        log.info("Obteniendo usuarios paginados - Página: {}, Tamaño: {}, Rol: {}, Estado: {}, Búsqueda: {}",
                page, size, role, status, search);

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        Role roleEnum = null;
        if (role != null && !role.trim().isEmpty()) {
            try {
                roleEnum = Role.valueOf(role.toUpperCase());
            } catch (IllegalArgumentException e) {
                log.warn("Rol inválido recibido: {}. Se ignorará el filtro de rol.", role);
            }
        }

        Long identification = null;
        try {
            if (search != null && !search.trim().isEmpty()) {
                identification = Long.parseLong(search.trim());
            }
        } catch (NumberFormatException e) {
        }

        Page<User> usersPage;
        boolean hasRole = roleEnum != null;
        boolean hasStatus = status != null && !status.trim().isEmpty();
        boolean hasSearch = search != null && !search.trim().isEmpty();

        if (hasSearch && hasRole && hasStatus) {
            boolean isActive = "active".equals(status);
            usersPage = userRepository.searchUsersByRoleAndStatus(roleEnum, isActive, search, identification, pageable);
        } else if (hasSearch && hasRole) {
            usersPage = userRepository.searchUsersByRole(roleEnum, search, identification, pageable);
        } else if (hasSearch && hasStatus) {
            boolean isActive = "active".equals(status);
            usersPage = userRepository.searchUsersByStatus(isActive, search, identification, pageable);
        } else if (hasSearch) {
            usersPage = userRepository.searchUsers(search, identification, pageable);
        } else if (hasRole && hasStatus) {
            boolean isActive = "active".equals(status);
            usersPage = userRepository.findByRoleAndIsActive(roleEnum, isActive, pageable);
        } else if (hasRole) {
            usersPage = userRepository.findByRole(roleEnum, pageable);
        } else if (hasStatus) {
            boolean isActive = "active".equals(status);
            usersPage = userRepository.findByIsActive(isActive, pageable);
        } else {
            usersPage = userRepository.findAll(pageable);
        }

        return usersPage;
    }

    @Transactional
    public User createUser(User user) {
        log.info("Creando nuevo usuario: {}", user.getEmail());

        if (userRepository.findByEmail(user.getEmail()).isPresent()) {
            throw new RuntimeException("El email ya está registrado");
        }

        if (user.getIdentification() != null &&
                userRepository.findByIdentification(user.getIdentification()).isPresent()) {
            throw new RuntimeException("La identificación ya está registrada");
        }

        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());

        User saved = userRepository.save(user);
        log.info("Usuario creado exitosamente con ID: {}", saved.getId());
        return saved;
    }

    @Transactional
    public User updateUser(String id, User updatedUser) {
        log.info("Actualizando usuario con ID: {}", id);

        User existingUser = getUserByIdOrThrow(id);

        if (!existingUser.getEmail().equals(updatedUser.getEmail())) {
            Optional<User> userWithEmail = userRepository.findByEmail(updatedUser.getEmail());
            if (userWithEmail.isPresent() && !userWithEmail.get().getId().equals(id)) {
                throw new RuntimeException("El email ya está en uso por otro usuario");
            }
        }

        if (updatedUser.getIdentification() != null &&
                !updatedUser.getIdentification().equals(existingUser.getIdentification())) {
            Optional<User> userWithId = userRepository.findByIdentification(updatedUser.getIdentification());
            if (userWithId.isPresent() && !userWithId.get().getId().equals(id)) {
                throw new RuntimeException("La identificación ya está en uso por otro usuario");
            }
        }

        existingUser.setName(updatedUser.getName());
        existingUser.setLastname(updatedUser.getLastname());
        existingUser.setEmail(updatedUser.getEmail());
        existingUser.setIdentification(updatedUser.getIdentification());
        existingUser.setPhone(updatedUser.getPhone());
        existingUser.setSex(updatedUser.getSex());
        existingUser.setBirthDate(updatedUser.getBirthDate());
        existingUser.setBloodType(updatedUser.getBloodType());
        existingUser.setRole(updatedUser.getRole());
        existingUser.setPlan(updatedUser.getPlan());
        existingUser.setActive(updatedUser.isActive());
        existingUser.setUpdatedAt(LocalDateTime.now());

        User saved = userRepository.save(existingUser);
        log.info("Usuario actualizado exitosamente: {}", saved.getEmail());
        return saved;
    }

    @Transactional
    public void updateUserRole(String id, Role role) { // Cambiado de String a Role
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con ID: " + id));
        user.setRole(role);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
    }

    @Transactional
    public void deleteById(String id) {
        log.debug("Eliminando usuario con ID: {}", id);
        User user = getUserByIdOrThrow(id);
        userRepository.delete(user);
        log.info("Usuario eliminado exitosamente: {}", user.getEmail());
    }

    public Optional<User> getUserById(String id) {
        return findById(id);
    }

    @Transactional
    public void updateProfilePicture(String email, MultipartFile file) throws Exception {
        User user = getUserOrThrow(email);
        Long idNumber = user.getIdentification();

        if (file != null && !file.isEmpty() && idNumber != null) {

            String userFolder = UPLOAD_DIRECTORY + "/" + idNumber;
            Path targetFolder = Paths.get(userFolder);

            if (!Files.exists(targetFolder)) {
                Files.createDirectories(targetFolder);
            }

            String fileExtension = getFileExtension(file.getOriginalFilename());
            String fileName = idNumber + "_profile" + fileExtension;
            Path targetPath = targetFolder.resolve(fileName);

            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

            String relativeUrl = "/uploads/profiles/" + idNumber + "/" + fileName;
            user.setPhotoProfile(relativeUrl);
            user.setUpdatedAt(LocalDateTime.now());

            userRepository.save(user);
        } else {
            throw new Exception("File is empty or User identification is missing");
        }
    }

    private String getFileExtension(String fileName) {
        if (fileName == null || !fileName.contains("."))
            return ".png";
        return fileName.substring(fileName.lastIndexOf("."));
    }
}
