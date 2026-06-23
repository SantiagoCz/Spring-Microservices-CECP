package com.santiagocz.employees_service.services;

import com.santiagocz.employees_service.domain.entities.Attendance;
import com.santiagocz.employees_service.domain.entities.Employee;
import com.santiagocz.employees_service.dto.attendance.AttendanceRequestDto;
import com.santiagocz.employees_service.dto.attendance.AttendanceResponseDto;
import com.santiagocz.employees_service.exceptions.EntityConflictException;
import com.santiagocz.employees_service.exceptions.EntityNotFoundException;
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
        validateEmployeeHasNoOpenCheckInToday(employee.getId());

        Attendance attendance = attendanceRepository.save(
                Attendance.builder()
                        .employee(employee)
                        .checkIn(LocalDateTime.now())
                        .build());

        return buildResponseDto(attendance);
    }

    @Transactional
    public AttendanceResponseDto createManual(AttendanceRequestDto dto) {
        Employee employee = employeeService.getEmployeeById(dto.getEmployeeId());
        validateCheckInBeforeCheckOut(dto.getCheckIn(), dto.getCheckOut());
        Attendance attendance = attendanceRepository.save(
                Attendance.builder()
                        .employee(employee)
                        .checkIn(dto.getCheckIn())
                        .checkOut(dto.getCheckOut())
                        .notes(dto.getNotes())
                        .build());
        return buildResponseDto(attendance);
    }

    // ──────────── READ ────────────

    @Transactional(readOnly = true)
    public AttendanceResponseDto findById(Long id) {
        return buildResponseDto(getAttendanceById(id));
    }

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

    @Transactional(readOnly = true)
    public List<AttendanceResponseDto> findOpenCheckInsBeforeToday() {
        return attendanceRepository.findOpenCheckInsBeforeToday(LocalDate.now().atStartOfDay())
                .stream().map(this::buildResponseDto).toList();
    }

    // ──────────── UPDATE ────────────

    @Transactional
    public AttendanceResponseDto checkOut(String dni, String notes) {
        Employee employee = employeeService.getEmployeeByDni(dni);
        Attendance attendance = getOpenCheckInTodayByEmployee(employee);

        attendance.setCheckOut(LocalDateTime.now());
        attendance.setNotes(notes);

        return buildResponseDto(attendance);
    }

    @Transactional
    public AttendanceResponseDto updateManual(Long id, AttendanceRequestDto dto) {
        Attendance attendance = getAttendanceById(id);
        validateCheckInBeforeCheckOut(dto.getCheckIn(), dto.getCheckOut());

        attendance.setCheckIn(dto.getCheckIn());
        attendance.setCheckOut(dto.getCheckOut());
        attendance.setNotes(dto.getNotes());
        return buildResponseDto(attendance);
    }

    // ──────────── PRIVATES ────────────

    private Attendance getAttendanceById(Long id) {
        return attendanceRepository.findByIdFetchEmployee(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "No se encontró la asistencia con ID: " + id));
    }

    private void validateEmployeeHasNoOpenCheckInToday(Long employeeId) {
        LocalDate today = LocalDate.now();
        if (attendanceRepository.existsByEmployeeIdAndCheckOutIsNullAndCheckInBetween(
                employeeId, today.atStartOfDay(), today.atTime(LocalTime.MAX))) {
            throw new EntityConflictException(
                    "Ya tiene una entrada abierta sin registrar la salida");
        }
    }

    private Attendance getOpenCheckInTodayByEmployee(Employee employee) {
        LocalDate today = LocalDate.now();
        return attendanceRepository
                .findOpenCheckInToday(employee.getId(), today.atStartOfDay(), today.atTime(LocalTime.MAX))
                .orElseThrow(() -> new EntityConflictException(
                        "No tiene una entrada abierta de hoy para cerrar. "
                                + "Si quedó una entrada sin cerrar de un día anterior, "
                                + "debe regularizarla con RRHH."));
    }

    private void validateCheckInBeforeCheckOut(LocalDateTime checkIn, LocalDateTime checkOut) {
        if (checkOut != null && checkOut.isBefore(checkIn)) {
            throw new EntityConflictException("La salida no puede ser anterior a la entrada");
        }
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