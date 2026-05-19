package com.santiagocz.affiliates_service.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PrimarySummaryDto {
    private Long id;
    private String dni;
    private String firstName;
    private String lastName;
}
