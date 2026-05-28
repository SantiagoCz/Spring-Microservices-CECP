package com.santiagocz.dental_service.services;

import com.santiagocz.dental_service.domain.entities.PriceList;
import com.santiagocz.dental_service.domain.entities.Professional;
import com.santiagocz.dental_service.dto.priceList.PriceListResponseDto;
import com.santiagocz.dental_service.dto.professional.ProfessionalRequestDto;
import com.santiagocz.dental_service.dto.professional.ProfessionalResponseDto;
import com.santiagocz.dental_service.exceptions.EntityNotFoundException;
import com.santiagocz.dental_service.repositories.ProfessionalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProfessionalService {

    private final ProfessionalRepository professionalRepository;
    private final PriceListService priceListService;

    // ──────────── CREATE ────────────

    @Transactional
    public ProfessionalResponseDto create(ProfessionalRequestDto dto) {
        PriceList priceList = priceListService.getEntityById(dto.priceListId());
        Professional professional = new Professional();
        professional.setName(formatWords(dto.firstName()) + " " + formatWords(dto.lastName()));
        professional.setPriceList(priceList);
        return toResponse(professionalRepository.save(professional));
    }

    // ──────────── READ ────────────

    @Transactional(readOnly = true)
    public ProfessionalResponseDto findById(Long id) {
        return toResponse(getEntityById(id));
    }

    @Transactional(readOnly = true)
    public List<ProfessionalResponseDto> findAll() {
        return professionalRepository.findAllByOrderByNameAsc().stream()
                .map(this::toResponse)
                .toList();
    }

    // ──────────── UPDATE ────────────

    @Transactional
    public ProfessionalResponseDto update(Long id, ProfessionalRequestDto dto) {
        Professional professional = getEntityById(id);
        PriceList priceList = priceListService.getEntityById(dto.priceListId());
        professional.setName(formatWords(dto.firstName()) + " " + formatWords(dto.lastName()));
        professional.setPriceList(priceList);
        return toResponse(professional);
    }

    // ──────────── DELETE ────────────

    @Transactional
    public void delete(Long id) {
        getEntityById(id);
        professionalRepository.deleteById(id);
    }

    // ──────────── PRIVATES ────────────

    public Professional getEntityById(Long id) {
        return professionalRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Profesional no encontrado con id: " + id));
    }

    public ProfessionalResponseDto toResponse(Professional professional) {
        return new ProfessionalResponseDto(
                professional.getId(),
                professional.getName(),
                new PriceListResponseDto(professional.getPriceList().getId(), professional.getPriceList().getName())
        );
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