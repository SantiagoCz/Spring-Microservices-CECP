package com.santiagocz.common.persistence;

import com.santiagocz.common.auth.AuthenticatedUser;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorAware", modifyOnCreate = false)
public class JpaAuditingConfig {

    private static final String SYSTEM = "system";

    @Bean
    public AuditorAware<Auditor> auditorAware() {
        return () -> {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            if (authentication != null
                    && authentication.isAuthenticated()
                    && authentication.getDetails() instanceof AuthenticatedUser user) {
                return Optional.of(new Auditor(user.id(), user.fullName()));
            }

            return Optional.of(new Auditor(null, SYSTEM));
        };
    }
}