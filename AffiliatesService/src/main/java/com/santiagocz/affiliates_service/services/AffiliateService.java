package com.santiagocz.affiliates_service.services;

import com.santiagocz.affiliates_service.component.AffiliateMapper;
import com.santiagocz.affiliates_service.domain.entities.Affiliate;
import com.santiagocz.affiliates_service.domain.enums.AffiliateType;
import com.santiagocz.affiliates_service.domain.enums.Status;
import com.santiagocz.affiliates_service.dto.AffiliateRequestDto;
import com.santiagocz.affiliates_service.dto.AffiliateResponseDto;
import com.santiagocz.affiliates_service.exceptions.AffiliateConflictException;
import com.santiagocz.affiliates_service.exceptions.AffiliateNotFoundException;
import com.santiagocz.affiliates_service.repositories.AffiliateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Period;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AffiliateService {

    private final AffiliateRepository affiliateRepository;
    private final AffiliateMapper mapper;

    // ──────────── CREATE ────────────

    @Transactional
    public AffiliateResponseDto createPrimary(AffiliateRequestDto dto) {
        validateDniNotInUse(dto.getDni());
        validateAdult(dto.getBirthDate());

        Affiliate primary = Affiliate.builder()
                .dni(dto.getDni())
                .firstName(formatWords(dto.getFirstName()))
                .lastName(formatWords(dto.getLastName()))
                .phoneNumber(dto.getPhoneNumber())
                .birthDate(dto.getBirthDate())
                .status(Status.ACTIVE)
                .affiliateType(AffiliateType.PRIMARY)
                .build();

        return mapper.toResponse(affiliateRepository.save(primary));
    }

    @Transactional
    public AffiliateResponseDto createDependent(Long primaryId, AffiliateRequestDto dto) {
        if (dto.getRelation() == null) {
            throw new AffiliateConflictException("Debe especificarse la relación con el titular");
        }

        Affiliate primary = getEntityById(primaryId);
        validatePrimaryRole(primary);
        validatePrimaryNotInactive(primary);
        validateDniNotInUse(dto.getDni());

        String phone = (dto.getPhoneNumber() == null || dto.getPhoneNumber().isBlank())
                ? primary.getPhoneNumber()
                : dto.getPhoneNumber();

        Affiliate dependent = Affiliate.builder()
                .dni(dto.getDni())
                .firstName(formatWords(dto.getFirstName()))
                .lastName(formatWords(dto.getLastName()))
                .phoneNumber(phone)
                .birthDate(dto.getBirthDate())
                .status(Status.ACTIVE)
                .affiliateType(AffiliateType.DEPENDENT)
                .relation(dto.getRelation())
                .primaryAffiliate(primary)
                .build();

        return mapper.toResponse(affiliateRepository.save(dependent));
    }

    // ──────────── READ ────────────

    @Transactional(readOnly = true)
    public AffiliateResponseDto getById(Long id) {
        return mapper.toResponse(getEntityById(id));
    }

    @Transactional(readOnly = true)
    public AffiliateResponseDto getByDni(String dni) {
        Affiliate a = affiliateRepository.findByDni(dni)
                .orElseThrow(() -> new AffiliateNotFoundException(
                        "No se encontró afiliado con DNI: " + dni));
        return mapper.toResponse(a);
    }

    @Transactional(readOnly = true)
    public Page<AffiliateResponseDto> listAll(Pageable pageable) {
        return affiliateRepository.findAllByOrderByLastNameAsc(pageable)
                .map(mapper::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<AffiliateResponseDto> listPrimaries(Pageable pageable) {
        return affiliateRepository
                .findByAffiliateTypeOrderByLastNameAsc(AffiliateType.PRIMARY, pageable)
                .map(mapper::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<AffiliateResponseDto> search(String term, Pageable pageable) {
        return affiliateRepository
                .findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCase(term, term, pageable)
                .map(mapper::toResponse);
    }

    @Transactional(readOnly = true)
    public AffiliateResponseDto getPrimaryWithFamily(Long primaryId) {
        Affiliate primary = affiliateRepository.findPrimaryWithFamilyById(primaryId)
                .orElseThrow(() -> new AffiliateNotFoundException(
                        "No se encontró titular con ID: " + primaryId));
        return mapper.toResponseWithFamily(primary);
    }

    @Transactional(readOnly = true)
    public List<AffiliateResponseDto> getFamilyGroup(Long affiliateId) {
        Affiliate affiliate = getEntityById(affiliateId);
        Long primaryId = (affiliate.getAffiliateType() == AffiliateType.PRIMARY)
                ? affiliate.getId()
                : affiliate.getPrimaryAffiliate().getId();
        return affiliateRepository.findFamilyGroupByPrimaryId(primaryId)
                .stream().map(mapper::toResponse).toList();
    }

    // ──────────── UPDATE ────────────

    @Transactional
    public AffiliateResponseDto update(Long id, AffiliateRequestDto dto) {
        Affiliate affiliate = getEntityById(id);

        if (affiliate.getStatus() == Status.INACTIVE) {
            throw new AffiliateConflictException("No se puede modificar un afiliado inactivo");
        }
        if (affiliate.getAffiliateType() == AffiliateType.PRIMARY) {
            validateAdult(dto.getBirthDate());
        }
        if (!affiliate.getDni().equals(dto.getDni())) {
            validateDniNotInUse(dto.getDni());
        }

        affiliate.setDni(dto.getDni());
        affiliate.setFirstName(formatWords(dto.getFirstName()));
        affiliate.setLastName(formatWords(dto.getLastName()));
        affiliate.setPhoneNumber(dto.getPhoneNumber());
        affiliate.setBirthDate(dto.getBirthDate());

        if (affiliate.getAffiliateType() == AffiliateType.DEPENDENT && dto.getRelation() != null) {
            affiliate.setRelation(dto.getRelation());
        }

        return mapper.toResponse(affiliate);
    }

    // ──────────── STATUS ────────────

    @Transactional
    public void deactivate(Long id) {
        Affiliate affiliate = getEntityById(id);
        if (affiliate.getStatus() == Status.INACTIVE) {
            throw new AffiliateConflictException("El afiliado ya está inactivo");
        }
        affiliate.setStatus(Status.INACTIVE);

        if (affiliate.getAffiliateType() == AffiliateType.PRIMARY) {
            affiliateRepository.findByPrimaryAffiliateIdOrderByLastNameAsc(id)
                    .stream()
                    .filter(d -> d.getStatus() != Status.INACTIVE)
                    .forEach(d -> d.setStatus(Status.INACTIVE));
        }
    }

    @Transactional
    public void activate(Long id) {
        Affiliate affiliate = getEntityById(id);
        if (affiliate.getStatus() == Status.ACTIVE) {
            throw new AffiliateConflictException("El afiliado ya está activo");
        }
        if (affiliate.getAffiliateType() == AffiliateType.DEPENDENT
                && affiliate.getPrimaryAffiliate().getStatus() == Status.INACTIVE) {
            throw new AffiliateConflictException("Primero debe darse de alta al titular");
        }
        affiliate.setStatus(Status.ACTIVE);
    }

    // ──────────── PRIVATES ────────────

    private Affiliate getEntityById(Long id) {
        return affiliateRepository.findById(id)
                .orElseThrow(() -> new AffiliateNotFoundException(
                        "No se encontró al afiliado con ID: " + id));
    }

    private void validateDniNotInUse(String dni) {
        if (affiliateRepository.existsByDni(dni)) {
            throw new AffiliateConflictException("Ya existe un afiliado con el DNI: " + dni);
        }
    }

    private void validateAdult(LocalDate birthDate) {
        if (Period.between(birthDate, LocalDate.now()).getYears() < 18) {
            throw new AffiliateConflictException("El afiliado titular debe ser mayor de edad");
        }
    }

    private void validatePrimaryRole(Affiliate affiliate) {
        if (affiliate.getAffiliateType() != AffiliateType.PRIMARY) {
            throw new AffiliateConflictException(
                    "El afiliado ID " + affiliate.getId() + " no es titular");
        }
    }

    private void validatePrimaryNotInactive(Affiliate primary) {
        if (primary.getStatus() == Status.INACTIVE) {
            throw new AffiliateConflictException(
                    "El titular está inactivo, no se puede agregar grupo familiar");
        }
    }

    private String formatWords(String text) {
        if (text == null || text.trim().isEmpty()) {
            return text;
        }

        String[] words = text.trim().split("\\s+");

        StringBuilder result = new StringBuilder();

        for (String word : words) {
            if (!word.isEmpty()) {
                String formattedWord = word.substring(0, 1).toUpperCase() +
                        word.substring(1).toLowerCase();

                if (result.length() > 0) {
                    result.append(" ");
                }
                result.append(formattedWord);
            }
        }

        return result.toString();
    }

}
