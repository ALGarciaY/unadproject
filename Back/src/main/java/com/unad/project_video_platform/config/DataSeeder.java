package com.unad.project_video_platform.config;

import com.unad.project_video_platform.entity.Role;
import com.unad.project_video_platform.entity.User;
import com.unad.project_video_platform.repository.RoleRepository;
import com.unad.project_video_platform.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DataSeeder implements ApplicationRunner {

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserRepository userRepository;

    // El login es correo + número de documento: en un servidor público el
    // admin inicial no puede ser el de la demo.
    @Value("${app.seed.admin-email:admin@demo.com}")
    private String adminEmail;

    @Value("${app.seed.admin-document:123456}")
    private String adminDocument;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        Role admin = ensureRole("ADMIN", "Administrador");
        ensureRole("USER", "Usuario final");
        ensureRole("MODERATOR", "Moderador");

        // Si no existe ningún usuario, crear un administrador inicial.
        if (userRepository.count() == 0) {
            User adminUser = new User();
            adminUser.setFirstName("Admin");
            adminUser.setLastName("Demo");
            adminUser.setEmail(adminEmail);
            adminUser.setDocumentType("CC");
            adminUser.setDocumentNumber(adminDocument);
            adminUser.setRole(admin);
            adminUser.setStatus(true);
            userRepository.save(adminUser);
        }
    }

    private Role ensureRole(String name, String description) {
        return roleRepository.findByRoleName(name).orElseGet(() -> {
            Role role = new Role();
            role.setRoleName(name);
            role.setDescription(description);
            return roleRepository.save(role);
        });
    }
}
