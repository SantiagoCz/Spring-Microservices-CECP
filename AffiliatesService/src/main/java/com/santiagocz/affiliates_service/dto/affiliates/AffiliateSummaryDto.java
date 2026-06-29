package com.santiagocz.affiliates_service.dto.affiliates;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AffiliateSummaryDto {
    private Long id;
    private String dni;
    private String firstName;
    private String lastName;
}