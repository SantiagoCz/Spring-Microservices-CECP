package com.santiagocz.dental_service.services;

import com.santiagocz.dental_service.domain.entities.PriceList;
import com.santiagocz.dental_service.dto.priceList.PriceListRequestDto;
import com.santiagocz.dental_service.dto.priceList.PriceListResponseDto;
import com.santiagocz.dental_service.exceptions.EntityConflictException;
import com.santiagocz.dental_service.exceptions.EntityNotFoundException;
import com.santiagocz.dental_service.repositories.PriceListRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PriceListService {

    private final PriceListRepository priceListRepository;

    // ──────────── CREATE ────────────

    @Transactional
    public PriceListResponseDto create(PriceListRequestDto dto) {
        if (priceListRepository.findByName(dto.name()).isPresent()) {
            throw new EntityConflictException(
                    "Ya existe una lista de precios con el nombre: " + dto.name());
        }
        PriceList priceList = new PriceList();
        priceList.setName(dto.name());
        return toResponse(priceListRepository.save(priceList));
    }

    // ──────────── READ ────────────

    @Transactional(readOnly = true)
    public PriceListResponseDto findById(Long id) {
        return toResponse(getEntityById(id));
    }

    @Transactional(readOnly = true)
    public List<PriceListResponseDto> findAll() {
        return priceListRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    // ──────────── UPDATE ────────────

    @Transactional
    public PriceListResponseDto update(Long id, PriceListRequestDto dto) {
        PriceList priceList = getEntityById(id);
        priceListRepository.findByName(dto.name())
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> { throw new EntityConflictException(
                        "Ya existe una lista de precios con el nombre: " + dto.name()); });
        priceList.setName(dto.name());
        return toResponse(priceList);
    }

    // ──────────── DELETE ────────────

    @Transactional
    public void delete(Long id) {
        getEntityById(id);
        priceListRepository.deleteById(id);
    }

    // ──────────── PRIVATES ────────────

    public PriceList getEntityById(Long id) {
        return priceListRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Lista de precios no encontrada con id: " + id));
    }

    private PriceListResponseDto toResponse(PriceList priceList) {
        return new PriceListResponseDto(
                priceList.getId(),
                priceList.getName());
    }
}