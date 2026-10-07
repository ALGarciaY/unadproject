package com.unad.project_video_platform.controller;

import com.unad.project_video_platform.entity.Role;
import com.unad.project_video_platform.service.impl.IRoleService;
import com.unad.project_video_platform.dto.ApiResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/roles")
public class RoleController {

    @Autowired
    private IRoleService roleService;

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<ApiResponse<List<Role>>> getAllRoles() {
        List<Role> roles = roleService.getAllRoles();
        return ResponseEntity.ok(ApiResponse.ok("Roles consulted", roles));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{id}")
        public ResponseEntity<ApiResponse<Role>> getRoleById(@PathVariable Integer id) {
        return roleService.getRoleById(id)
            .map(role -> ResponseEntity.ok(ApiResponse.ok("Role found", role)))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.<Role>notFound("Role not found with id: " + id)));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/search")
        public ResponseEntity<ApiResponse<Role>> getRoleByName(@RequestParam String name) {
        return roleService.getRoleByName(name)
            .map(role -> ResponseEntity.ok(ApiResponse.ok("Role found", role)))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.<Role>notFound("Role not found with name: " + name)));
    }

    /**
     * POST /api/roles - Crea un nuevo rol
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<ApiResponse<Role>> createRole(@RequestBody Role role) {
        try {
            Role createdRole = roleService.createRole(role);
            return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created("Role created", createdRole));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.<Role>badRequest(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.<Role>internalError("Error creating role"));
        }
    }

    /**
     * PUT /api/roles/{id} - Actualiza un rol existente
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Role>> updateRole(@PathVariable Integer id, @RequestBody Role roleDetails) {
        try {
            Role updatedRole = roleService.updateRole(id, roleDetails);
            return ResponseEntity.ok(ApiResponse.ok("Role updated", updatedRole));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.<Role>badRequest(e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.<Role>notFound(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.<Role>internalError("Error updating role"));
        }
    }

    /**
     * DELETE /api/roles/{id} - Elimina un rol
     */
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteRole(@PathVariable Integer id) {
        try {
            roleService.deleteRole(id);
            return ResponseEntity.ok(ApiResponse.<Void>ok("Role deleted successfully", null));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.<Void>notFound(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.<Void>internalError("Error deleting role"));
        }
    }

    /**
     * GET /api/roles/exists?name={roleName} - Verifica si existe un rol
     */
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/exists")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> existsByRoleName(@RequestParam String name) {
        boolean exists = roleService.existsByRoleName(name);
        return ResponseEntity.ok(ApiResponse.ok(Map.of("exists", exists)));
    }
}
