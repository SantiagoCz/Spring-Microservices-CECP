package com.santiagocz.auth_service.services;

import com.santiagocz.auth_service.domain.entities.User;
import com.santiagocz.auth_service.dto.request.LoginRequest;
import com.santiagocz.auth_service.dto.response.AuthResponse;
import com.santiagocz.auth_service.dto.response.PersonResponse;
import com.santiagocz.auth_service.dto.response.TokenResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserService userService;

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
            );

            User user = userService.getUserByUsername(request.getUsername());

            String accessToken = jwtService.generateAccessToken(user);
            String refreshToken = jwtService.generateRefreshToken(user);

            TokenResponse tokenResponse = TokenResponse.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .tokenType("Bearer")
                    .expiresIn(3600L)
                    .build();

            PersonResponse personResponse = userService.getPersonByUsername(request.getUsername());

            return AuthResponse.builder()
                    .username(user.getUsername())
                    .hierarchyRole(user.getHierarchyRole())
                    .token(tokenResponse)
                    .person(personResponse)
                    .build();

        } catch (AuthenticationException e) {
            throw new IllegalArgumentException("DNI o contraseña incorrectos", e);
        }
    }

    @Transactional(readOnly = true)
    public AuthResponse refreshToken(String refreshToken) {
        try {
            String username = jwtService.extractUsername(refreshToken);
            User user = userService.getUserByUsername(username);

            if (!jwtService.isTokenValid(refreshToken, user)) {
                throw new IllegalArgumentException("Token de refresco inválido o expirado");
            }

            String newAccessToken = jwtService.generateAccessToken(user);

            TokenResponse tokenResponse = TokenResponse.builder()
                    .accessToken(newAccessToken)
                    .refreshToken(refreshToken)
                    .tokenType("Bearer")
                    .expiresIn(3600L)
                    .build();

            PersonResponse personResponse = userService.getPersonByUsername(username);

            return AuthResponse.builder()
                    .username(user.getUsername())
                    .hierarchyRole(user.getHierarchyRole())
                    .token(tokenResponse)
                    .person(personResponse)
                    .build();

        } catch (Exception e) {
            throw new IllegalArgumentException("Error al refrescar token: " + e.getMessage(), e);
        }
    }
}