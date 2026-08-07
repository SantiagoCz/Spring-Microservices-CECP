package com.santiagocz.auth_service.controllers;

import com.santiagocz.auth_service.dto.request.LoginRequest;
import com.santiagocz.auth_service.dto.request.RegisterRequest;
import com.santiagocz.auth_service.dto.response.ApiResponse;
import com.santiagocz.auth_service.dto.response.RegisterResponse;
import com.santiagocz.auth_service.services.AuthService;
import com.santiagocz.auth_service.services.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserService userService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse> login(@Valid @RequestBody LoginRequest request) {
        var authResponse = authService.login(request);
        return ResponseEntity.ok(new ApiResponse(200, "Inicio de sesión exitoso", authResponse));
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse> register(@Valid @RequestBody RegisterRequest request) {
        RegisterResponse response = userService.registerUser(request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse(201, "Usuario registrado exitosamente", response));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse> refreshToken(@RequestHeader("Authorization") String authHeader) {
        String refreshToken = extractTokenFromHeader(authHeader);
        var authResponse = authService.refreshToken(refreshToken);
        return ResponseEntity.ok(new ApiResponse(200, "Token refrescado exitosamente", authResponse));
    }

    @GetMapping("/validate")
    public ResponseEntity<ApiResponse> validateToken(@RequestHeader("Authorization") String authHeader) {
        extractTokenFromHeader(authHeader);
        return ResponseEntity.ok(new ApiResponse(200, "Token válido", null));
    }

    private String extractTokenFromHeader(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Header Authorization inválido");
        }
        return authHeader.substring(7);
    }
}