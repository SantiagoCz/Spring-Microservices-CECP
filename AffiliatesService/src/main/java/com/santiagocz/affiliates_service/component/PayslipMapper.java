package com.santiagocz.affiliates_service.component;

import com.santiagocz.affiliates_service.domain.entities.Affiliate;
import com.santiagocz.affiliates_service.domain.entities.Payslip;
import com.santiagocz.affiliates_service.dto.paylips.PayslipResponseDto;
import org.springframework.stereotype.Component;

@Component
public class PayslipMapper {
    public PayslipResponseDto toResponse(Payslip payslip) {
        Affiliate primary = payslip.getPrimaryAffiliate();
        return PayslipResponseDto.builder()
                .id(payslip.getId())
                .period(payslip.getPeriod())
                .uploadDate(payslip.getUploadDate())
                .primaryAffiliateId(primary.getId())
                .primaryAffiliateName(primary.getFirstName() + " " + primary.getLastName())
                .fileName(payslip.getFileName())
                .build();
    }
}