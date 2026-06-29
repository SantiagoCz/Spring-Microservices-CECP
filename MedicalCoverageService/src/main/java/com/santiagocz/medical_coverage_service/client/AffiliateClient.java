package com.santiagocz.medical_coverage_service.client;

import com.santiagocz.medical_coverage_service.dto.affiliate.AffiliateSummaryDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@FeignClient(name = "affiliate-service")
public interface AffiliateClient {

    @GetMapping("/api/affiliates/{id}/active")
    Boolean isActive(@PathVariable("id") Long id);

    @PostMapping("/api/affiliates/lookup")
    List<AffiliateSummaryDto> lookupByIds(@RequestBody List<Long> ids);
}