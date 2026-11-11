package com.santiagocz.medical_coverage_service.client;

import com.santiagocz.common.exceptions.ServiceUnavailableException;
import com.santiagocz.medical_coverage_service.dto.affiliate.AffiliateSummaryDto;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class AffiliateGateway {

    private final AffiliateClient affiliateClient;

    @CircuitBreaker(name = "affiliate-service", fallbackMethod = "lookupFallback")
    public List<AffiliateSummaryDto> lookupByIds(List<Long> ids) {
        return affiliateClient.lookupByIds(ids);
    }

    @CircuitBreaker(name = "affiliate-service", fallbackMethod = "isActiveFallback")
    public Boolean isActive(Long id) {
        return affiliateClient.isActive(id);
    }

    // Read: si Affiliates falla, no se puede armar el pago completo, así que se rechaza con 503.
    List<AffiliateSummaryDto> lookupFallback(List<Long> ids, Throwable throwable) {
        log.warn("Servicio de Afiliados no disponible: {}", throwable.getClass().getSimpleName());
        throw new ServiceUnavailableException(
                "No se pueden mostrar pagos: el Servicio de Afiliados no está disponible.", throwable);
    }

    // Create: si Affiliates falla, no podemos confirmar el afiliado, así que se rechaza con 503.
    Boolean isActiveFallback(Long id, Throwable throwable) {
        log.warn("Servicio de Afiliados no disponible al validar afiliado {}: {}",
                id, throwable.getClass().getSimpleName());
        throw new ServiceUnavailableException(
                "No se puede registrar el pago: el Servicio de Afiliados no está disponible.", throwable);
    }
}