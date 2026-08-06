package com.santiagocz.api_gateway.filter;

import com.santiagocz.api_gateway.services.JwtService;
import lombok.RequiredArgsConstructor;
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
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    private final JwtService jwtService;

    private static final List<String> PUBLIC_ROUTES = List.of(
            "/auth/login",
            "/auth/register",
            "/auth/refresh",
            "/auth/validate",
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

                // Inyectar headers para que los microservicios los lean
                ServerWebExchange mutatedExchange = exchange.mutate()
                        .request(r -> r
                                .header("X-User-Name", username)
                                .header("X-Internal-Secret", "tu-secreto-interno")
                        )
                        .build();

                return chain.filter(mutatedExchange);
            } else {
                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                return exchange.getResponse().setComplete();
            }

        } catch (Exception e) {
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
        return -1;  // Ejecutar primero
    }
}