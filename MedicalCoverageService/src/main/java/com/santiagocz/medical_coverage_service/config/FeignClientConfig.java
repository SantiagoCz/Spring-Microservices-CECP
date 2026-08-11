package com.santiagocz.medical_coverage_service.config;

import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;

@Configuration
public class FeignClientConfig {

    @Bean
    public RequestInterceptor requestInterceptor() {
        return requestTemplate -> {
            // Obtener la request actual
            ServletRequestAttributes attributes =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();

                // Propagar headers de seguridad
                String username = request.getHeader("X-User-Name");
                String roles = request.getHeader("X-User-Roles");
                String internalSecret = request.getHeader("X-Internal-Secret");

                if (username != null) {
                    requestTemplate.header("X-User-Name", username);
                }
                if (roles != null) {
                    requestTemplate.header("X-User-Roles", roles);
                }
                if (internalSecret != null) {
                    requestTemplate.header("X-Internal-Secret", internalSecret);
                }
            }
        };
    }
}