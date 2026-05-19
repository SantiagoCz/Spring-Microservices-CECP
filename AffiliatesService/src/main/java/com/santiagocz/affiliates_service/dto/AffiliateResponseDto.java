package com.santiagocz.affiliates_service.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.santiagocz.affiliates_service.domain.enums.AffiliateType;
import com.santiagocz.affiliates_service.domain.enums.RelationType;
import com.santiagocz.affiliates_service.domain.enums.Status;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AffiliateResponseDto {

    private Long id;
    private String dni;
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private LocalDate birthDate;
    private Status status;
    private AffiliateType affiliateType;

    // Sólo presentes si es DEPENDENT
    private RelationType relation;
    private PrimarySummaryDto primaryAffiliate;

    // Sólo presente cuando se pide explícitamente (endpoint del grupo)
    private List<AffiliateResponseDto> familyMembers;
}
