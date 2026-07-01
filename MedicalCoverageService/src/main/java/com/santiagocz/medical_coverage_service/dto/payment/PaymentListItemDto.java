package com.santiagocz.medical_coverage_service.dto.payment;

import com.santiagocz.medical_coverage_service.domain.enums.Status;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class PaymentListItemDto {
    private Long id;
    private LocalDate date;
    private Long medicalOrderNumber;
    private String affiliateDni;
    private String affiliateFullName;
    private Double amount;
    private Integer discount;
    private Double discountAmount;
    private Status status;
}