package com.santiagocz.affiliates_service.dto.paylips;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PayslipResponseDto {
    private Long id;
    private LocalDate period;
    private LocalDateTime uploadDate;
    private Long primaryAffiliateId;
    private String primaryAffiliateName;
    private String fileName;
}