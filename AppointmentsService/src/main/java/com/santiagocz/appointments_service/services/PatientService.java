package com.santiagocz.appointments_service.services;

import com.santiagocz.appointments_service.domain.entities.Patient;
import com.santiagocz.appointments_service.domain.enums.Status;
import com.santiagocz.appointments_service.dto.patient.PatientRequestDto;
import com.santiagocz.appointments_service.dto.patient.PatientResponseDto;
import com.santiagocz.appointments_service.exceptions.EntityConflictException;
import com.santiagocz.appointments_service.exceptions.EntityNotFoundException;
import com.santiagocz.appointments_service.repositories.PatientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PatientService {

    private final PatientRepository patientRepository;

    // ──────────── CREATE ────────────

    @Transactional
    public PatientResponseDto create(PatientRequestDto dto) {
        validateDniNotInUse(dto.getDni());

        Patient patient = buildEntity(dto);
        patient.setStatus(Status.ACTIVE);
        return buildResponseDto(patientRepository.save(patient));
    }

    // ──────────── READ ────────────

    @Transactional(readOnly = true)
    public PatientResponseDto getById(Long id) {
        return buildResponseDto(getEntityById(id));
    }

    @Transactional(readOnly = true)
    public PatientResponseDto getByDni(String dni) {
        Patient patient = patientRepository.findByDni(dni)
                .orElseThrow(() -> new EntityNotFoundException(
                        "No se encontró paciente con DNI: " + dni));
        return buildResponseDto(patient);
    }

    @Transactional(readOnly = true)
    public Page<PatientResponseDto> listAll(Pageable pageable) {
        return patientRepository.findAllByOrderByLastNameAsc(pageable)
                .map(this::buildResponseDto);
    }

    @Transactional(readOnly = true)
    public Page<PatientResponseDto> search(String term, Pageable pageable) {
        return patientRepository
                .findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCase(term, term, pageable)
                .map(this::buildResponseDto);
    }

    // ──────────── UPDATE ────────────

    @Transactional
    public PatientResponseDto update(Long id, PatientRequestDto dto) {
        Patient patient = getEntityById(id);

        if (patient.getStatus() == Status.INACTIVE) {
            throw new EntityConflictException("No se puede modificar un paciente inactivo");
        }

        if (!patient.getDni().equals(dto.getDni())) {
            validateDniNotInUse(dto.getDni());
        }

        patient.setDni(dto.getDni());
        patient.setFirstName(formatWords(dto.getFirstName()));
        patient.setLastName(formatWords(dto.getLastName()));
        patient.setPhoneNumber(dto.getPhoneNumber());
        patient.setBirthDate(dto.getBirthDate());

        return buildResponseDto(patient);
    }

    // ──────────── STATUS ────────────

    @Transactional
    public void deactivate(Long id) {
        Patient patient = getEntityById(id);
        if (patient.getStatus() == Status.INACTIVE) {
            throw new EntityConflictException("Paciente ya inactivo");
        }
        patient.setStatus(Status.INACTIVE);
    }

    @Transactional
    public void activate(Long id) {
        Patient patient = getEntityById(id);
        if (patient.getStatus() == Status.ACTIVE) {
            throw new EntityConflictException("Paciente ya activo");
        }

        patient.setStatus(Status.ACTIVE);
    }

    // ──────────── PRIVATES ────────────

    private Patient getEntityById(Long id) {
        return patientRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "No se encontró al paciente con ID: " + id));
    }

    private void validateDniNotInUse(String dni) {
        if (patientRepository.existsByDni(dni)) {
            throw new EntityConflictException("Ya existe un paciente con el DNI: " + dni);
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
    private PatientResponseDto buildResponseDto(Patient patient) {
        return PatientResponseDto.builder()
                .id(patient.getId())
                .dni(patient.getDni())
                .firstName(patient.getFirstName())
                .lastName(patient.getLastName())
                .phoneNumber(patient.getPhoneNumber())
                .birthDate(patient.getBirthDate())
                .status(patient.getStatus())
                .build();
    }

    private Patient buildEntity(PatientRequestDto dto) {
        return Patient.builder()
                .dni(dto.getDni())
                .firstName(formatWords(dto.getFirstName()))
                .lastName(formatWords(dto.getLastName()))
                .phoneNumber(dto.getPhoneNumber())
                .birthDate(dto.getBirthDate())
                .build();
    }

}