package com.santiagocz.affiliates_service.component;

import com.santiagocz.affiliates_service.dto.affiliates.AffiliateResponseDto;
import com.santiagocz.affiliates_service.dto.affiliates.PrimarySummaryDto;
import com.santiagocz.affiliates_service.domain.entities.Affiliate;
import com.santiagocz.affiliates_service.domain.enums.AffiliateType;
import org.springframework.stereotype.Component;

@Component
public class AffiliateMapper {

    // Versión básica: para listados y operaciones simples
    public AffiliateResponseDto toResponse(Affiliate a) {
        if (a == null) return null;

        AffiliateResponseDto.AffiliateResponseDtoBuilder builder = AffiliateResponseDto.builder()
                .id(a.getId())
                .dni(a.getDni())
                .firstName(a.getFirstName())
                .lastName(a.getLastName())
                .phoneNumber(a.getPhoneNumber())
                .birthDate(a.getBirthDate())
                .status(a.getStatus())
                .affiliateType(a.getAffiliateType());

        if (a.getAffiliateType() == AffiliateType.DEPENDENT) {
            builder.relation(a.getRelation());
            builder.primaryAffiliate(toPrimarySummary(a.getPrimaryAffiliate()));
        }

        return builder.build();
    }

    // Versión detallada: si es titular, incluye la lista de dependientes
    public AffiliateResponseDto toResponseWithFamily(Affiliate primary) {
        AffiliateResponseDto dto = toResponse(primary);
        if (primary.getAffiliateType() == AffiliateType.PRIMARY
                && primary.getFamilyMembers() != null) {
            dto.setFamilyMembers(
                    primary.getFamilyMembers().stream().map(this::toResponse).toList()
            );
        }
        return dto;
    }

    private PrimarySummaryDto toPrimarySummary(Affiliate primary) {
        if (primary == null) return null;
        return PrimarySummaryDto.builder()
                .id(primary.getId())
                .dni(primary.getDni())
                .firstName(primary.getFirstName())
                .lastName(primary.getLastName())
                .build();
    }
}
