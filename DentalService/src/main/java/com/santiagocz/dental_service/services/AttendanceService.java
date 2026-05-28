package com.santiagocz.dental_service.services;

import com.santiagocz.dental_service.domain.entities.Attendance;
import com.santiagocz.dental_service.domain.entities.AttendanceItem;
import com.santiagocz.dental_service.domain.entities.Code;
import com.santiagocz.dental_service.domain.entities.Professional;
import com.santiagocz.dental_service.dto.attendance.AttendanceItemRequestDto;
import com.santiagocz.dental_service.dto.attendance.AttendanceItemResponseDto;
import com.santiagocz.dental_service.dto.attendance.AttendanceRequestDto;
import com.santiagocz.dental_service.dto.attendance.AttendanceResponseDto;
import com.santiagocz.dental_service.dto.code.CodeResponseDto;
import com.santiagocz.dental_service.exceptions.EntityConflictException;
import com.santiagocz.dental_service.exceptions.EntityNotFoundException;
import com.santiagocz.dental_service.repositories.AttendanceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final ProfessionalService professionalService;
    private final CodeService codeService;

    // ──────────── CREATE ────────────

    @Transactional
    public AttendanceResponseDto create(AttendanceRequestDto dto) {
        Professional professional = professionalService.getEntityById(dto.professionalId());

        if (dto.items().isEmpty()) {
            throw new EntityConflictException(
                    "El bono debe contener al menos un código.");
        }

        Attendance attendance = new Attendance();
        attendance.setDate(dto.date());
        attendance.setVoucherNumber(dto.voucherNumber());
        attendance.setProfessional(professional);
        attendance.setAppointmentId(dto.appointmentId());

        for (AttendanceItemRequestDto itemRequest : dto.items()) {
            Code code = codeService.getEntityById(itemRequest.codeId());
            AttendanceItem item = new AttendanceItem();
            item.setAttendance(attendance);
            item.setCode(code);
            item.setToothSurface(itemRequest.toothSurface());
            attendance.getItems().add(item);
        }

        return toResponse(attendanceRepository.save(attendance));
    }

    // ──────────── READ ────────────

    @Transactional(readOnly = true)
    public AttendanceResponseDto findById(Long id) {
        return toResponse(getEntityById(id));
    }

    // ──────────── DELETE ────────────

    @Transactional
    public void delete(Long id) {
        getEntityById(id);
        attendanceRepository.deleteById(id);
    }


    // ──────────── PRIVATES ────────────

    private Attendance getEntityById(Long id) {
        return attendanceRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Bono no encontrado con id: " + id));
    }

    private AttendanceResponseDto toResponse(Attendance attendance) {
        List<AttendanceItemResponseDto> items = attendance.getItems().stream()
                .map(item -> new AttendanceItemResponseDto(
                        item.getId(),
                        new CodeResponseDto(item.getCode().getId(), item.getCode().getNumber(), item.getCode().getDescription()),
                        item.getToothSurface()
                ))
                .toList();

        return new AttendanceResponseDto(
                attendance.getId(),
                attendance.getDate(),
                attendance.getVoucherNumber(),
                professionalService.toResponse(attendance.getProfessional()),
                attendance.getAppointmentId(),
                items
        );
    }

}