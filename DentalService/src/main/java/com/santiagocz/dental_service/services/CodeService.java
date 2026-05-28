package com.santiagocz.dental_service.services;

import com.santiagocz.dental_service.domain.entities.Code;
import com.santiagocz.dental_service.dto.code.CodeRequestDto;
import com.santiagocz.dental_service.dto.code.CodeResponseDto;
import com.santiagocz.dental_service.exceptions.EntityConflictException;
import com.santiagocz.dental_service.exceptions.EntityNotFoundException;
import com.santiagocz.dental_service.repositories.CodeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CodeService {

    private final CodeRepository codeRepository;

    // ──────────── CREATE ────────────

    @Transactional
    public CodeResponseDto create(CodeRequestDto dto) {
        if (codeRepository.findByNumber(dto.number()).isPresent()) {
            throw new EntityConflictException(
                    "Ya existe un código con el número: " + dto.number());
        }
        Code code = new Code();
        code.setNumber(dto.number());
        code.setDescription(dto.description());
        return toResponse(codeRepository.save(code));
    }

    // ──────────── READ ────────────

    @Transactional(readOnly = true)
    public CodeResponseDto findById(Long id) {
        return toResponse(getEntityById(id));
    }

    @Transactional(readOnly = true)
    public List<CodeResponseDto> findAll() {
        return codeRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    // ──────────── UPDATE ────────────

    @Transactional
    public CodeResponseDto update(Long id, CodeRequestDto dto) {
        Code code = getEntityById(id);
        codeRepository.findByNumber(dto.number())
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> { throw new EntityConflictException(
                        "Ya existe un código con el número: " + dto.number()); });
        code.setNumber(dto.number());
        code.setDescription(dto.description());
        return toResponse(code);
    }

    // ──────────── DELETE ────────────

    @Transactional
    public void delete(Long id) {
        getEntityById(id);
        codeRepository.deleteById(id);
    }

    // ──────────── PRIVATES ────────────

    public Code getEntityById(Long id) {
        return codeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Código no encontrado con id: " + id));
    }

    private CodeResponseDto toResponse(Code code) {
        return new CodeResponseDto(
                code.getId(),
                code.getNumber(),
                code.getDescription()
        );
    }
}