package com.santiagocz.medical_coverage_service.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class HeaderAuthenticationFilter extends OncePerRequestFilter {

    @Value("${internal.secret}")
    private String internalSecret;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        try {
            String username = request.getHeader("X-User-Name");
            String rolesHeader = request.getHeader("X-User-Roles");
            String internalSecretHeader = request.getHeader("X-Internal-Secret");

            log.info("=== HeaderAuthenticationFilter ===");
            log.info("X-User-Name: {}", username);
            log.info("X-User-Roles: {}", rolesHeader);
            log.info("X-Internal-Secret: {}", internalSecretHeader != null ? "✓ present" : "✗ missing");
            log.info("internal.secret config: {}", internalSecret);

            if (username != null && internalSecretHeader != null) {

                if (!internalSecretHeader.equals(internalSecret)) {
                    log.warn("Internal secret NO coincide!");
                    filterChain.doFilter(request, response);
                    return;
                }

                log.info("✓ Internal secret coincide");

                // TODO: Validar que el usuario no esté eliminado

                List<SimpleGrantedAuthority> authorities = new ArrayList<>();
                if (rolesHeader != null && !rolesHeader.isEmpty()) {
                    String[] roles = rolesHeader.split(",");
                    for (String role : roles) {
                        String trimmedRole = role.trim();
                        authorities.add(new SimpleGrantedAuthority(trimmedRole));
                        log.info("  Added authority: {}", trimmedRole);
                    }
                }

                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(
                                username,
                                null,
                                authorities
                        );

                SecurityContextHolder.getContext().setAuthentication(authToken);
                log.info("✓ Usuario {} autenticado con {} authorities", username, authorities.size());
            } else {
                log.warn("Falta X-User-Name o X-Internal-Secret");
            }
        } catch (Exception e) {
            log.error("Error en HeaderAuthenticationFilter: ", e);
        }

        filterChain.doFilter(request, response);
    }
}