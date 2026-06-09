package com.santiagocz.affiliates_service.controllers;

import com.santiagocz.affiliates_service.dto.ApiResponse;
import com.santiagocz.affiliates_service.dto.paylips.PayslipRequestDto;
import com.santiagocz.affiliates_service.dto.paylips.PayslipResponseDto;
import com.santiagocz.affiliates_service.services.PayslipService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/payslips")
@RequiredArgsConstructor
public class PayslipController {

    private final PayslipService payslipService;

    // ──────────── CREATE ────────────

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<PayslipResponseDto> upload(
            @Valid @ModelAttribute PayslipRequestDto dto,
            @RequestParam("file") MultipartFile file) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(payslipService.upload(dto, file));
    }

    // ──────────── READ ────────────

    @GetMapping("/{id}")
    public ResponseEntity<PayslipResponseDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(payslipService.getById(id));
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<byte[]> download(@PathVariable Long id) {
        byte[] content = payslipService.downloadFile(id);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"payslip-" + id + ".pdf\"")
                .body(content);
    }

    @GetMapping("/by-affiliate/{affiliateId}/latest")
    public ResponseEntity<PayslipResponseDto> latest(@PathVariable Long affiliateId) {
        return ResponseEntity.ok(payslipService.getLatestByAffiliate(affiliateId));
    }

    @GetMapping("/by-affiliate/{affiliateId}")
    public ResponseEntity<List<PayslipResponseDto>> listByAffiliate(@PathVariable Long affiliateId) {
        return ResponseEntity.ok(payslipService.getAllByAffiliate(affiliateId));
    }

    // ──────────── DELETE ────────────

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse> delete(@PathVariable Long id) {
        payslipService.delete(id);
        return ResponseEntity.ok(
                new ApiResponse(HttpStatus.OK.value(), "Recibo de sueldo eliminado correctamente."));
    }
}