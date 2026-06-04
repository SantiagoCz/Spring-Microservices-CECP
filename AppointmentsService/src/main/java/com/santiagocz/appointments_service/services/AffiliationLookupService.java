package com.santiagocz.appointments_service.services;

import com.santiagocz.appointments_service.clients.AffiliateClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AffiliationLookupService {

    private final AffiliateClient affiliateClient;

    public Boolean isActive(String dni) {
        if (dni == null || dni.isBlank()) return null;
        Optional<Set<String>> result = fetch(List.of(dni));
        return result.map(set -> set.contains(dni)).orElse(null);
    }

    public Optional<Set<String>> activeDnisFromBatch(List<String> dnis) {
        if (dnis == null || dnis.isEmpty()) return Optional.of(Set.of());
        return fetch(dnis);
    }

    private Optional<Set<String>> fetch(List<String> dnis) {
        try {
            return Optional.of(affiliateClient.filterActiveDnis(dnis));
        } catch (Exception ex) {
            return Optional.empty();
        }
    }
}