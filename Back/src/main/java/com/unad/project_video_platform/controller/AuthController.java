package com.unad.project_video_platform.controller;

import com.unad.project_video_platform.dto.LoginRequest;
import com.unad.project_video_platform.dto.LoginResponse;
import com.unad.project_video_platform.dto.ApiResponse;
import com.unad.project_video_platform.dto.RecoverPasswordRequest;
import com.unad.project_video_platform.service.impl.IAuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private IAuthService authService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@RequestBody LoginRequest request) {
        try {
            LoginResponse response = authService.login(request);
            return ResponseEntity.ok(ApiResponse.ok("Login successful", response));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.<LoginResponse>badRequest(e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.<LoginResponse>unauthorized(e.getMessage()));
        }
    }

    @PostMapping("/recover-password")
    public ResponseEntity<ApiResponse<Void>> recoverPassword(@RequestBody RecoverPasswordRequest request) {
        try {
            authService.sendPasswordRecoveryEmail(request);
            return ResponseEntity.ok(ApiResponse.<Void>ok("Password sent successfully", null));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.<Void>badRequest(e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.<Void>internalError(e.getMessage()));
        }
    }
}
