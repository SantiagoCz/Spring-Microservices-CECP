package com.santiagocz.auth_service.controllers;

import com.santiagocz.auth_service.dto.request.LoginRequest;
import com.santiagocz.auth_service.dto.request.RegisterRequest;
import com.santiagocz.auth_service.dto.request.ResetPasswordRequest;
import com.santiagocz.auth_service.dto.request.UpdatePasswordRequest;
import com.santiagocz.auth_service.dto.response.ApiResponse;
import com.santiagocz.auth_service.dto.response.UserResponse;
import com.santiagocz.auth_service.services.AuthService;
import com.santiagocz.auth_service.services.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserService userService;

    // ──────────── AUTHENTICATION ────────────

    @PostMapping("/login")
    public ResponseEntity<ApiResponse> login(@Valid @RequestBody LoginRequest request) {
        var authResponse = authService.login(request);
        return ResponseEntity.ok(new ApiResponse(200, "Inicio de sesión exitoso", authResponse));
    }

    @PostMapping("/register")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> register(@Valid @RequestBody RegisterRequest request) {
        UserResponse response = userService.registerUser(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse(201, "Usuario registrado exitosamente", response));
    }

    @PostMapping("/refresh")
    @PreAuthorize("isAuthenticated()")  // Cualquiera logueado
    public ResponseEntity<ApiResponse> refreshToken(@RequestHeader("Authorization") String authHeader) {
        String refreshToken = extractTokenFromHeader(authHeader);
        var authResponse = authService.refreshToken(refreshToken);
        return ResponseEntity.ok(new ApiResponse(200, "Token refrescado exitosamente", authResponse));
    }

    @GetMapping("/validate")
    @PreAuthorize("isAuthenticated()")  // Cualquiera logueado
    public ResponseEntity<ApiResponse> validateToken(@RequestHeader("Authorization") String authHeader) {
        extractTokenFromHeader(authHeader);
        return ResponseEntity.ok(new ApiResponse(200, "Token válido", null));
    }

    // ──────────── READ ────────────

    @GetMapping("/{userId}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> getUserById(@PathVariable Long userId) {
        return ResponseEntity.ok(new ApiResponse(200, "Usuario obtenido", userService.getUserById(userId)));
    }

    @GetMapping
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> listUsers(@PageableDefault(size = 20, sort = "username") Pageable pageable) {
        return ResponseEntity.ok(new ApiResponse(200, "Usuarios obtenidos", userService.listUsers(pageable)));
    }

    // ──────────── UPDATE - PASSWORD - SUBROLES ────────────

    @PatchMapping("/me/password")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse> updateMyPassword(@Valid @RequestBody UpdatePasswordRequest request) {
        userService.updateMyPassword(request);
        return ResponseEntity.ok(new ApiResponse(200, "Contraseña actualizada correctamente", null));
    }

    @PatchMapping("/{userId}/password/reset")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> resetUserPassword(@PathVariable Long userId,
                                                         @Valid @RequestBody ResetPasswordRequest request) {
        userService.resetUserPassword(userId, request.getNewPassword());
        return ResponseEntity.ok(new ApiResponse(200, "Contraseña restablecida correctamente", null));
    }

    @PostMapping("/{userId}/subroles/{subrolName}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")  // Por ahora solo SUPER_ADMIN (después se complica para ADMIN)
    public ResponseEntity<ApiResponse> addSubrol(
            @PathVariable Long userId,
            @PathVariable String subrolName) {
        userService.addSubrolToUser(userId, subrolName);
        return ResponseEntity.ok(
                new ApiResponse(200, "Subrol agregado correctamente", null)
        );
    }

    @DeleteMapping("/{userId}/subroles/{subrolName}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")  // Por ahora solo SUPER_ADMIN
    public ResponseEntity<ApiResponse> removeSubrol(
            @PathVariable Long userId,
            @PathVariable String subrolName) {
        userService.removeSubrolFromUser(userId, subrolName);
        return ResponseEntity.ok(
                new ApiResponse(200, "Subrol removido correctamente", null)
        );
    }

    // ──────────── DELETE - RESTORE ────────────

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")  // Por ahora solo SUPER_ADMIN
    public ResponseEntity<ApiResponse> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.ok(
                new ApiResponse(HttpStatus.OK.value(), "Usuario se ha dado de baja correctamente.", null));
    }

    @PatchMapping("/{id}/restore")
    @PreAuthorize("hasRole('SUPER_ADMIN')")  // Por ahora solo SUPER_ADMIN
    public ResponseEntity<ApiResponse> restoreUser(@PathVariable Long id) {
        userService.restoreUser(id);
        return ResponseEntity.ok(
                new ApiResponse(HttpStatus.OK.value(), "Usuario se ha dado de alta correctamente.", null));
    }

    // ──────────── PRIVATE HELPERS ────────────

    private String extractTokenFromHeader(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Header Authorization inválido");
        }
        return authHeader.substring(7);
    }
}