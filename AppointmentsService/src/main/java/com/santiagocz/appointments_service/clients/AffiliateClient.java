package com.santiagocz.appointments_service.clients;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;
import java.util.Set;

@FeignClient(name = "affiliate-service")
public interface AffiliateClient {

    @PostMapping("/api/affiliates/active-dnis")
    Set<String> filterActiveDnis(@RequestBody List<String> dnis);
}