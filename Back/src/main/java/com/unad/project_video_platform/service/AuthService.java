package com.unad.project_video_platform.service;

import com.unad.project_video_platform.dto.LoginRequest;
import com.unad.project_video_platform.dto.LoginResponse;
import com.unad.project_video_platform.dto.RecoverPasswordRequest;
import com.unad.project_video_platform.entity.User;
import com.unad.project_video_platform.repository.UserRepository;
import com.unad.project_video_platform.security.JwtService;
import com.unad.project_video_platform.service.impl.IAuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class AuthService implements IAuthService {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AuthService.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String mailUsername;

    @Override
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("Invalid credentials: Email or password incorrect"));

        if (!Boolean.TRUE.equals(user.getStatus())
            || request.getDocumentNumber() == null
            || !request.getDocumentNumber().equals(user.getDocumentNumber())) {
            throw new IllegalArgumentException("Invalid credentials: Email or password incorrect");
        }

        String roleName = user.getRole() != null ? user.getRole().getRoleName() : "USER";
        String token = jwtService.generateToken(user.getEmail(), roleName);

        return new LoginResponse(token, "Bearer", roleName, user.getId());
    }

    @Override
    public void sendPasswordRecoveryEmail(RecoverPasswordRequest request) {
        if (request.getEmail() == null || request.getEmail().isBlank()) {
            throw new IllegalArgumentException("El correo electronico es obligatorio");
        }

        User user = userRepository.findByEmail(request.getEmail().trim()).orElse(null);
        if (user == null || !Boolean.TRUE.equals(user.getStatus())) {
            return;
        }

        if (mailUsername == null || mailUsername.isBlank()) {
            log.error("Recuperacion solicitada pero MAIL_USERNAME no esta configurado");
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(mailUsername);
            message.setTo(user.getEmail());
            message.setSubject("Recuperacion de contrasena - GuiaFacil");
            message.setText("""
                    Hola %s,

                    Recibimos una solicitud para recuperar tu contrasena.

                    Tu clave de acceso actual es:
                    %s

                    Si no solicitaste este mensaje, puedes ignorarlo.
                    """.formatted(user.getFirstName(), user.getDocumentNumber()));

            mailSender.send(message);
        } catch (MailException e) {
            log.error("No se pudo enviar el correo de recuperacion", e);
        }
    }
}
