package com.santiagocz.common.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Slf4j
public class HeaderAuthenticationFilter extends OncePerRequestFilter {

    private static final String USER_HEADER = "X-User-Name";
    private static final String ROLES_HEADER = "X-User-Roles";
    private static final String SECRET_HEADER = "X-Internal-Secret";

    @Value("${internal.secret}")
    private String internalSecret;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        try {
            String username = request.getHeader(USER_HEADER);
            String rolesHeader = request.getHeader(ROLES_HEADER);
            String secret = request.getHeader(SECRET_HEADER);

            if (username != null && secret != null && secret.equals(internalSecret)) {

                // TODO: Validar que el usuario no esté eliminado

                List<SimpleGrantedAuthority> authorities = parseAuthorities(rolesHeader);
                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(username, null, authorities);
                SecurityContextHolder.getContext().setAuthentication(authToken);
            } else if (username != null && secret != null) {
                log.warn("Invalid internal secret for user: {}", username);
            }
        } catch (Exception e) {
           log.error("Error processing authentication headers", e);
        }

        filterChain.doFilter(request, response);
    }

    private List<SimpleGrantedAuthority> parseAuthorities(String rolesHeader) {
        if (rolesHeader == null || rolesHeader.isBlank()) {
            return List.of();
        }
        return Arrays.stream(rolesHeader.split(","))
                .map(String::trim)
                .filter(role -> !role.isEmpty())
                .map(SimpleGrantedAuthority::new)
                .toList();
    }
}