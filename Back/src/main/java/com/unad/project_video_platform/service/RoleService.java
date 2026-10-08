package com.unad.project_video_platform.service;

import com.unad.project_video_platform.entity.Role;
import com.unad.project_video_platform.repository.RoleRepository;
import com.unad.project_video_platform.service.impl.IRoleService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class RoleService implements IRoleService {

    @Autowired
    private RoleRepository roleRepository;

    /**
     * Obtiene todos los roles
     */
    public List<Role> getAllRoles() {
        return roleRepository.findAll();
    }

    /**
     * Obtiene un rol por ID
     */
    public Optional<Role> getRoleById(Integer id) {
        return roleRepository.findById(id);
    }

    /**
     * Obtiene un rol por nombre
     */
    public Optional<Role> getRoleByName(String roleName) {
        return roleRepository.findByRoleName(roleName);
    }

    /**
     * Crea un nuevo rol
     */
    @Transactional
    public Role createRole(Role role) {
        if (role.getRoleName() == null || role.getRoleName().isBlank()) {
            throw new IllegalArgumentException("El nombre del rol es obligatorio");
        }
        role.setRoleName(role.getRoleName().trim());
        if (role.getDescription() != null) {
            role.setDescription(role.getDescription().trim());
        }

        if (roleRepository.existsByRoleName(role.getRoleName())) {
            throw new IllegalArgumentException("Ya existe un rol con el nombre: " + role.getRoleName());
        }
        return roleRepository.save(role);
    }

    /**
     * Actualiza un rol existente
     */
    @Transactional
    public Role updateRole(Integer id, Role roleDetails) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Rol no encontrado con id: " + id));

        if (roleDetails.getRoleName() == null || roleDetails.getRoleName().isBlank()) {
            throw new IllegalArgumentException("El nombre del rol es obligatorio");
        }
        String roleName = roleDetails.getRoleName().trim();

        // Validar que el nombre no esté en uso por otro rol
        if (!role.getRoleName().equals(roleName) && roleRepository.existsByRoleName(roleName)) {
            throw new IllegalArgumentException("Ya existe un rol con el nombre: " + roleName);
        }

        role.setRoleName(roleName);
        role.setDescription(roleDetails.getDescription() != null ? roleDetails.getDescription().trim() : null);

        return roleRepository.save(role);
    }

    /**
     * Elimina un rol por ID
     */
    @Transactional
    public void deleteRole(Integer id) {
        if (!roleRepository.existsById(id)) {
            throw new RuntimeException("Rol no encontrado con id: " + id);
        }
        roleRepository.deleteById(id);
    }

    /**
     * Verifica si existe un rol por nombre
     */
    public boolean existsByRoleName(String roleName) {
        return roleRepository.existsByRoleName(roleName);
    }
}
