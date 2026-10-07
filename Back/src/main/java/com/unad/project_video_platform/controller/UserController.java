package com.unad.project_video_platform.controller;

import com.unad.project_video_platform.dto.ApiResponse;
import com.unad.project_video_platform.dto.ProfileUpdateRequest;
import com.unad.project_video_platform.entity.User;
import com.unad.project_video_platform.service.impl.IUserService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private IUserService userService;

    @GetMapping("/profile/current")
    public ResponseEntity<ApiResponse<User>> getCurrentUser(Authentication authentication) {
        String email = authentication.getName();
        return userService.getUserByEmail(email)
                .map(user -> ResponseEntity.ok(ApiResponse.ok("Current user found", user)))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.<User>notFound("User not found with email: " + email)));
    }

    @PutMapping("/profile/current")
    public ResponseEntity<ApiResponse<User>> updateCurrentUser(
            Authentication authentication,
            @RequestBody User userDetails) {
        try {
            User updatedUser = userService.updateProfile(authentication.getName(), userDetails);
            return ResponseEntity.ok(ApiResponse.ok("Profile updated", updatedUser));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.<User>badRequest(e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.<User>notFound(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.<User>internalError(e.getMessage()));
        }
    }

    /**
     * GET /api/users - Obtiene todos los usuarios
     */
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<ApiResponse<List<User>>> getAllUsers() {
        List<User> users = userService.getAllUsers();
        return ResponseEntity.ok(ApiResponse.ok("Users consulted", users));
    }

    /**
     * GET /api/users/me - Obtiene el perfil del usuario autenticado
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<User>> getCurrentUser() {
        try {
            User user = userService.getCurrentUser();
            return ResponseEntity.ok(ApiResponse.ok("Current user", user));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.<User>unauthorized(e.getMessage()));
        }
    }

    /**
     * PUT /api/users/me - Actualiza los datos del perfil del usuario autenticado
     */
    @PutMapping("/me")
    public ResponseEntity<ApiResponse<User>> updateCurrentUser(@RequestBody ProfileUpdateRequest request) {
        try {
            User updated = userService.updateProfile(request);
            return ResponseEntity.ok(ApiResponse.ok("Profile updated", updated));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.<User>badRequest(e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.<User>notFound(e.getMessage()));
        }
    }

    /**
     * POST /api/users/me/photo - Actualiza la foto de perfil
     */
    @PostMapping("/me/photo")
    public ResponseEntity<ApiResponse<User>> uploadPhoto(@RequestParam("file") MultipartFile file) {
        try {
            User updated = userService.updatePhoto(file);
            return ResponseEntity.ok(ApiResponse.ok("Photo updated", updated));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.<User>badRequest(e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.<User>internalError(e.getMessage()));
        }
    }

    /**
     * GET /api/users/{id} - Obtiene un usuario por ID
     */
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{id}")
        public ResponseEntity<ApiResponse<User>> getUserById(@PathVariable Integer id) {
        return userService.getUserById(id)
            .map(user -> ResponseEntity.ok(ApiResponse.ok("User found", user)))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.<User>notFound("User not found with id: " + id)));
    }

    /**
     * GET /api/users/search/email?email={email} - Busca un usuario por email
     */
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/search/email")
        public ResponseEntity<ApiResponse<User>> getUserByEmail(@RequestParam String email) {
        return userService.getUserByEmail(email)
            .map(user -> ResponseEntity.ok(ApiResponse.ok("User found", user)))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.<User>notFound("User not found with email: " + email)));
    }

    /**
     * GET /api/users/search/document?documentNumber={documentNumber} - Busca un
     * usuario por documento
     */
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/search/document")
        public ResponseEntity<ApiResponse<User>> getUserByDocumentNumber(@RequestParam String documentNumber) {
        return userService.getUserByDocumentNumber(documentNumber)
            .map(user -> ResponseEntity.ok(ApiResponse.ok("User found", user)))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.<User>notFound("User not found with document: " + documentNumber)));
    }

    /**
     * GET /api/users/role/{roleId} - Obtiene usuarios por rol
     */
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/role/{roleId}")
    public ResponseEntity<ApiResponse<List<User>>> getUsersByRoleId(@PathVariable Integer roleId) {
        List<User> users = userService.getUsersByRoleId(roleId);
        return ResponseEntity.ok(ApiResponse.ok("Users by role consulted", users));
    }

    /**
     * POST /api/users - Crea un nuevo usuario
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<ApiResponse<User>> createUser(@RequestBody User user) {
        try {
            User createdUser = userService.createUser(user);
            return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created("User created", createdUser));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.<User>badRequest(e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.<User>notFound(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.<User>internalError(e.getMessage()));
        }
    }

    /**
     * PUT /api/users/{id} - Actualiza un usuario existente
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<User>> updateUser(@PathVariable Integer id, @RequestBody User userDetails) {
        try {
            User updatedUser = userService.updateUser(id, userDetails);
            return ResponseEntity.ok(ApiResponse.ok("User updated", updatedUser));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.<User>badRequest(e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.<User>notFound(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.<User>internalError(e.getMessage()));
        }
    }

    /**
     * DELETE /api/users/{id} - Elimina un usuario
     */
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable Integer id) {
        try {
            userService.deleteUser(id);
            return ResponseEntity.ok(ApiResponse.<Void>ok("User deleted successfully", null));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.<Void>notFound(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.<Void>internalError(e.getMessage()));
        }
    }

    /**
     * GET /api/users/exists/email?email={email} - Verifica si existe un email
     */
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/exists/email")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> existsByEmail(@RequestParam String email) {
        boolean exists = userService.existsByEmail(email);
        return ResponseEntity.ok(ApiResponse.ok(Map.of("exists", exists)));
    }

    /**
     * GET /api/users/exists/document?documentNumber={documentNumber} - Verifica si
     * existe un documento
     */
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/exists/document")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> existsByDocumentNumber(
            @RequestParam String documentNumber) {
        boolean exists = userService.existsByDocumentNumber(documentNumber);
        return ResponseEntity.ok(ApiResponse.ok(Map.of("exists", exists)));
    }
}
