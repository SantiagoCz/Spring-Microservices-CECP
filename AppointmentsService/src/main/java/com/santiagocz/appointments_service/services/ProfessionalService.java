package com.santiagocz.appointments_service.services;

import com.santiagocz.appointments_service.domain.entities.Professional;
import com.santiagocz.appointments_service.domain.enums.Status;
import com.santiagocz.appointments_service.dto.professional.ProfessionalRequestDto;
import com.santiagocz.appointments_service.dto.professional.ProfessionalResponseDto;
import com.santiagocz.appointments_service.exceptions.EntityConflictException;
import com.santiagocz.appointments_service.exceptions.EntityNotFoundException;
import com.santiagocz.appointments_service.repositories.ProfessionalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProfessionalService {

    private final ProfessionalRepository professionalRepository;

    // ──────────── CREATE ────────────

    @Transactional
    public ProfessionalResponseDto create(ProfessionalRequestDto dto) {
        validateDniNotInUse(dto.getDni());

        Professional professional = buildEntity(dto);
        professional.setStatus(Status.ACTIVE);
        return buildResponseDto(professionalRepository.save(professional));
    }

    // ──────────── READ ────────────

    @Transactional(readOnly = true)
    public ProfessionalResponseDto getById(Long id) {
        return buildResponseDto(getEntityById(id));
    }

    @Transactional(readOnly = true)
    public ProfessionalResponseDto getByDni(String dni) {
        Professional professional = professionalRepository.findByDni(dni)
                .orElseThrow(() -> new EntityNotFoundException(
                        "No se encontró profesional con DNI: " + dni));
        return buildResponseDto(professional);
    }

    @Transactional(readOnly = true)
    public Page<ProfessionalResponseDto> listAll(Pageable pageable) {
        return professionalRepository.findAllByOrderByLastNameAsc(pageable)
                .map(this::buildResponseDto);
    }

    @Transactional(readOnly = true)
    public Page<ProfessionalResponseDto> search(String term, Pageable pageable) {
        return professionalRepository
                .findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCase(term, term, pageable)
                .map(this::buildResponseDto);
    }

    // ──────────── UPDATE ────────────

    @Transactional
    public ProfessionalResponseDto update(Long id, ProfessionalRequestDto dto) {
        Professional professional = getEntityById(id);

        if (professional.getStatus() == Status.INACTIVE) {
            throw new EntityConflictException("No se puede modificar un profesional inactivo");
        }

        if (!professional.getDni().equals(dto.getDni())) {
            validateDniNotInUse(dto.getDni());
        }

        professional.setDni(dto.getDni());
        professional.setFirstName(formatWords(dto.getFirstName()));
        professional.setLastName(formatWords(dto.getLastName()));
        professional.setPhoneNumber(dto.getPhoneNumber());
        professional.setBirthDate(dto.getBirthDate());

        return buildResponseDto(professional);
    }

    // ──────────── STATUS ────────────

    @Transactional
    public void deactivate(Long id) {
        Professional professional = getEntityById(id);
        if (professional.getStatus() == Status.INACTIVE) {
            throw new EntityConflictException("Profesional ya inactivo");
        }
        professional.setStatus(Status.INACTIVE);
    }

    @Transactional
    public void activate(Long id) {
        Professional professional = getEntityById(id);
        if (professional.getStatus() == Status.ACTIVE) {
            throw new EntityConflictException("Profesional ya activo");
        }

        professional.setStatus(Status.ACTIVE);
    }

    // ──────────── PRIVATES ────────────

    private Professional getEntityById(Long id) {
        return professionalRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "No se encontró al profesional con ID: " + id));
    }

    private void validateDniNotInUse(String dni) {
        if (professionalRepository.existsByDni(dni)) {
            throw new EntityConflictException("Ya existe un profesional con el DNI: " + dni);
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

    // Mappers
    private ProfessionalResponseDto buildResponseDto(Professional professional) {
        return ProfessionalResponseDto.builder()
                .id(professional.getId())
                .dni(professional.getDni())
                .licenseNumber(professional.getLicenseNumber())
                .firstName(professional.getFirstName())
                .lastName(professional.getLastName())
                .phoneNumber(professional.getPhoneNumber())
                .birthDate(professional.getBirthDate())
                .specialty(professional.getSpecialty())
                .status(professional.getStatus())
                .build();
    }

    private Professional buildEntity(ProfessionalRequestDto dto) {
        return Professional.builder()
                .dni(dto.getDni())
                .licenseNumber((dto.getLicenseNumber()))
                .firstName(formatWords(dto.getFirstName()))
                .lastName(formatWords(dto.getLastName()))
                .phoneNumber(dto.getPhoneNumber())
                .birthDate(dto.getBirthDate())
                .specialty(dto.getSpecialty())
                .build();
    }

}