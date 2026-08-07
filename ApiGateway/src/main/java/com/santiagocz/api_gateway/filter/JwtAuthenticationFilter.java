package com.santiagocz.api_gateway.filter;

import com.santiagocz.api_gateway.services.JwtService;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    private final JwtService jwtService;

    @Value("${internal.secret}")
    private String internalSecret;

    private static final List<String> PUBLIC_ROUTES = List.of(
            "/auth-service/auth/login",
            "/auth-service/auth/register",
            "/auth-service/auth/refresh",
            "/auth-service/auth/validate",
            "/actuator"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();

        // Rutas públicas pasan sin validación
        if (isPublicRoute(path)) {
            return chain.filter(exchange);
        }

        try {
            String token = extractTokenFromRequest(exchange);

            if (token != null && jwtService.isTokenValid(token)) {
                String username = jwtService.extractUsername(token);

                // ← AGREGAR: Extraer roles del token
                Claims claims = jwtService.extractAllClaims(token);
                String roles = (String) claims.get("roles");

                log.info("Gateway: Usuario {} con roles: {}", username, roles);

                // Inyectar headers para que los microservicios los lean
                ServerWebExchange mutatedExchange = exchange.mutate()
                        .request(r -> r
                                .header("X-User-Name", username)
                                .header("X-User-Roles", roles != null ? roles : "")  // ← AGREGAR
                                .header("X-Internal-Secret", internalSecret)  // ← USAR CONFIG
                        )
                        .build();

                return chain.filter(mutatedExchange);
            } else {
                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                return exchange.getResponse().setComplete();
            }

        } catch (Exception e) {
            log.error("Error en JWT filter: ", e);
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }
    }

    private String extractTokenFromRequest(ServerWebExchange exchange) {
        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

        if (StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }

        return null;
    }

    private boolean isPublicRoute(String path) {
        return PUBLIC_ROUTES.stream().anyMatch(path::startsWith);
    }

    @Override
    public int getOrder() {
        return -1;  // Ejecuta primero
    }
}