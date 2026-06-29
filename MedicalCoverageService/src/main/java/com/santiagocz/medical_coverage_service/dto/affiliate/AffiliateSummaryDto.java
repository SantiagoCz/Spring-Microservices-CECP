package com.santiagocz.medical_coverage_service.dto.affiliate;

import lombok.Data;

@Data
public class AffiliateSummaryDto {
    private Long id;
    private String dni;
    private String firstName;
    private String lastName;
}