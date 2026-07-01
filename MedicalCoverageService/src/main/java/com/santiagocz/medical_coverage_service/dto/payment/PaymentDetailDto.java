package com.santiagocz.medical_coverage_service.dto.payment;

import com.santiagocz.medical_coverage_service.domain.enums.Delegation;
import com.santiagocz.medical_coverage_service.domain.enums.Status;
import com.santiagocz.medical_coverage_service.dto.affiliate.AffiliateSummaryDto;
import com.santiagocz.medical_coverage_service.dto.medicalOrder.MedicalOrderResponseDto;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class PaymentDetailDto {
    private Long id;
    private LocalDate date;
    private Double amount;
    private Integer discount;
    private Double discountAmount;
    private Status status;
    private AffiliateSummaryDto affiliate;
    private Long creatorId;
    private Delegation delegation;
    private MedicalOrderResponseDto medicalOrder;
}