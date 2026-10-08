package com.unad.project_video_platform.service;

import com.unad.project_video_platform.dto.ProfileUpdateRequest;
import com.unad.project_video_platform.entity.User;
import com.unad.project_video_platform.repository.RoleRepository;
import com.unad.project_video_platform.repository.UserRepository;
import com.unad.project_video_platform.service.impl.IUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;

@Service
public class UserService implements IUserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private FileStorageService fileStorageService;

    /**
     * Obtiene todos los usuarios
     */
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    /**
     * Obtiene un usuario por ID
     */
    public Optional<User> getUserById(Integer id) {
        return userRepository.findById(id);
    }

    /**
     * Obtiene un usuario por email
     */
    public Optional<User> getUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    /**
     * Obtiene un usuario por número de documento
     */
    public Optional<User> getUserByDocumentNumber(String documentNumber) {
        return userRepository.findByDocumentNumber(documentNumber);
    }

    /**
     * Obtiene usuarios por rol
     */
    public List<User> getUsersByRoleId(Integer roleId) {
        return userRepository.findByRoleId(roleId);
    }

    /**
     * Crea un nuevo usuario
     */
    @Transactional
    public User createUser(User user) {
        validateRequiredText(user.getFirstName(), "El nombre es obligatorio");
        validateRequiredText(user.getLastName(), "El apellido es obligatorio");
        validateRequiredText(user.getEmail(), "El correo electrónico es obligatorio");
        validateRequiredText(user.getDocumentNumber(), "El número de documento es obligatorio");

        user.setFirstName(user.getFirstName().trim());
        user.setLastName(user.getLastName().trim());
        user.setEmail(user.getEmail().trim());
        user.setDocumentNumber(user.getDocumentNumber().trim());

        // Validar email único
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new IllegalArgumentException("Ya existe un usuario con el email: " + user.getEmail());
        }

        // Validar documento único
        if (userRepository.existsByDocumentNumber(user.getDocumentNumber())) {
            throw new IllegalArgumentException("Ya existe un usuario con el número de documento: " + user.getDocumentNumber());
        }

        // Validar que el rol existe si se proporciona
        if (user.getRole() != null && user.getRole().getId() != null) {
            roleRepository.findById(user.getRole().getId())
                    .orElseThrow(() -> new RuntimeException("Rol no encontrado con id: " + user.getRole().getId()));
        }

        if (user.getStatus() == null) {
            user.setStatus(true);
        }

        return userRepository.save(user);
    }

    /**
     * Actualiza un usuario existente
     */
    @Transactional
    public User updateUser(Integer id, User userDetails) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con id: " + id));

        validateRequiredText(userDetails.getFirstName(), "El nombre es obligatorio");
        validateRequiredText(userDetails.getLastName(), "El apellido es obligatorio");
        validateRequiredText(userDetails.getEmail(), "El correo electrónico es obligatorio");

        String email = userDetails.getEmail().trim();

        // Validar email único (si cambió)
        if (!user.getEmail().equals(email) && userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Ya existe un usuario con el email: " + email);
        }

        // Validar que el rol existe si se proporciona
        if (userDetails.getRole() != null && userDetails.getRole().getId() != null) {
            roleRepository.findById(userDetails.getRole().getId())
                    .orElseThrow(() -> new RuntimeException("Rol no encontrado con id: " + userDetails.getRole().getId()));
        }

        // El numero de documento es la credencial de acceso: no se cambia desde
        // la edicion de usuarios, solo al crear la cuenta.
        user.setRole(userDetails.getRole());
        user.setFirstName(userDetails.getFirstName().trim());
        user.setLastName(userDetails.getLastName().trim());
        user.setDocumentType(userDetails.getDocumentType());
        user.setEmail(email);
        user.setStatus(userDetails.getStatus() != null ? userDetails.getStatus() : true);
        user.setPhone(userDetails.getPhone());
        user.setCargo(userDetails.getCargo());
        user.setBio(userDetails.getBio());
        user.setPhotoUrl(userDetails.getPhotoUrl());

        return userRepository.save(user);
    }

    /**
     * Actualiza datos editables del perfil propio.
     */
    @Transactional
    public User updateProfile(String email, User userDetails) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con email: " + email));

        validateRequiredText(userDetails.getFirstName(), "El nombre es obligatorio");
        validateRequiredText(userDetails.getLastName(), "El apellido es obligatorio");
        validateRequiredText(userDetails.getDocumentNumber(), "El numero de documento es obligatorio");

        String documentNumber = userDetails.getDocumentNumber().trim();

        if (!user.getDocumentNumber().equals(documentNumber)
                && userRepository.existsByDocumentNumber(documentNumber)) {
            throw new IllegalArgumentException(
                    "Ya existe un usuario con el numero de documento: " + documentNumber);
        }

        user.setFirstName(userDetails.getFirstName().trim());
        user.setLastName(userDetails.getLastName().trim());
        user.setDocumentType(userDetails.getDocumentType());
        user.setDocumentNumber(documentNumber);
        user.setPhone(userDetails.getPhone());
        user.setCargo(userDetails.getCargo());
        user.setBio(userDetails.getBio());

        return userRepository.save(user);
    }

    private void validateRequiredText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
    }

    private User getCurrentUserEntity() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            throw new RuntimeException("No hay sesion autenticada");
        }
        String email = auth.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado: " + email));
    }

    public User getCurrentUser() {
        return getCurrentUserEntity();
    }

    @Transactional
    public User updateProfile(ProfileUpdateRequest request) {
        User user = getCurrentUserEntity();
        if (request.getFirstName() != null && !request.getFirstName().isBlank()) {
            user.setFirstName(request.getFirstName());
        }
        if (request.getLastName() != null && !request.getLastName().isBlank()) {
            user.setLastName(request.getLastName());
        }
        user.setBio(request.getBio());
        user.setPhone(request.getPhone());
        user.setCargo(request.getCargo());
        return userRepository.save(user);
    }

    @Transactional
    public User updatePhoto(MultipartFile file) {
        User user = getCurrentUserEntity();
        String path = fileStorageService.storeImage(file);
        String previous = user.getPhotoUrl();
        user.setPhotoUrl(path);
        User saved = userRepository.save(user);
        fileStorageService.delete(previous);
        return saved;
    }

    /**
     * Elimina un usuario por ID
     */
    @Transactional
    public void deleteUser(Integer id) {
        if (!userRepository.existsById(id)) {
            throw new RuntimeException("Usuario no encontrado con id: " + id);
        }
        userRepository.deleteById(id);
    }

    /**
     * Verifica si existe un usuario por email
     */
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    /**
     * Verifica si existe un usuario por número de documento
     */
    public boolean existsByDocumentNumber(String documentNumber) {
        return userRepository.existsByDocumentNumber(documentNumber);
    }
}
