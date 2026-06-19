package com.santiagocz.employees_service.services;

import com.santiagocz.employees_service.domain.entities.Attendance;
import com.santiagocz.employees_service.domain.entities.Employee;
import com.santiagocz.employees_service.dto.attendance.AttendanceResponseDto;
import com.santiagocz.employees_service.exceptions.EntityConflictException;
import com.santiagocz.employees_service.repositories.AttendanceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final EmployeeService employeeService;

    // ──────────── CREATE ────────────

    @Transactional
    public AttendanceResponseDto checkIn(String dni) {
        Employee employee = employeeService.getEmployeeByDni(dni);
        employeeService.validateEmployeeIsActive(employee);
        validateEmployeeHasNotCheckedInToday(employee.getId());

        Attendance attendance = Attendance.builder()
                .employee(employee)
                .checkIn(LocalDateTime.now())
                .build();

        return buildResponseDto(attendanceRepository.save(attendance));
    }

    // ──────────── READ ────────────

    @Transactional(readOnly = true)
    public List<AttendanceResponseDto> findByEmployeeId(Long employeeId) {
        employeeService.validateEmployeeExists(employeeId);
        return attendanceRepository.findByEmployeeIdFetchEmployee(employeeId)
                .stream()
                .map(this::buildResponseDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AttendanceResponseDto> findByEmployeeIdBetweenDates(Long employeeId,
                                                                    LocalDateTime from,
                                                                    LocalDateTime to) {
        employeeService.validateEmployeeExists(employeeId);
        return attendanceRepository
                .findByEmployeeIdAndCheckInBetweenFetchEmployee(employeeId, from, to)
                .stream()
                .map(this::buildResponseDto)
                .toList();
    }

    // ──────────── UPDATE ────────────

    @Transactional
    public AttendanceResponseDto checkOut(String dni, String notes) {
        Employee employee = employeeService.getEmployeeByDni(dni);
        Attendance attendance = getOpenCheckInByEmployee(employee);

        attendance.setCheckOut(LocalDateTime.now());
        attendance.setNotes(notes);

        return buildResponseDto(attendance);
    }

    // ──────────── PRIVATES ────────────

    private void validateEmployeeHasNotCheckedInToday(Long employeeId) {
        LocalDate today = LocalDate.now();
        if (attendanceRepository.existsByEmployeeIdAndCheckInBetween(
                employeeId,
                today.atStartOfDay(),
                today.atTime(LocalTime.MAX))) {
            throw new EntityConflictException("Ya registró su entrada hoy");
        }
    }

    private Attendance getOpenCheckInByEmployee(Employee employee) {
        return attendanceRepository.findFirstByEmployeeIdAndCheckOutIsNull(employee.getId())
                .orElseThrow(() -> new EntityConflictException(
                        "No se registró entrada abierta para: " + employee.getFirstName() + " " + employee.getLastName()));
    }

    // Mapper
    private AttendanceResponseDto buildResponseDto(Attendance attendance) {
        return AttendanceResponseDto.builder()
                .id(attendance.getId())
                .employeeId(attendance.getEmployee().getId())
                .employeeFirstName(attendance.getEmployee().getFirstName())
                .employeeLastName(attendance.getEmployee().getLastName())
                .checkIn(attendance.getCheckIn())
                .checkOut(attendance.getCheckOut())
                .notes(attendance.getNotes())
                .build();
    }
}