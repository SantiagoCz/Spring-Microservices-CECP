package com.santiagocz.employees_service.services;

import com.santiagocz.employees_service.domain.entities.Employee;
import com.santiagocz.employees_service.domain.entities.Schedule;
import com.santiagocz.employees_service.domain.enums.EmployeeStatus;
import com.santiagocz.employees_service.dto.schedule.ScheduleRequestDto;
import com.santiagocz.employees_service.dto.schedule.ScheduleResponseDto;
import com.santiagocz.employees_service.exceptions.EntityConflictException;
import com.santiagocz.employees_service.exceptions.EntityNotFoundException;
import com.santiagocz.employees_service.repositories.ScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ScheduleService {

    private final ScheduleRepository scheduleRepository;
    private final EmployeeService employeeService;

    // ──────────── CREATE ────────────

    @Transactional
    public ScheduleResponseDto create(ScheduleRequestDto dto) {
        Employee employee = employeeService.getEmployeeById(dto.getEmployeeId());

        employeeService.validateEmployeeIsActive(employee);

        validateNoOverlap(dto.getEmployeeId(), dto.getDayOfWeek(),
                dto.getStartTime(), dto.getEndTime(), null);

        Schedule schedule = Schedule.builder()
                .employee(employee)
                .dayOfWeek(dto.getDayOfWeek())
                .startTime(dto.getStartTime())
                .endTime(dto.getEndTime())
                .build();

        return buildResponseDto(scheduleRepository.save(schedule));
    }

    @Transactional
    public List<ScheduleResponseDto> createBatch(List<ScheduleRequestDto> dtos) {
        return dtos.stream()
                .map(this::create)
                .toList();
    }

    // ──────────── READ ────────────

    @Transactional(readOnly = true)
    public List<ScheduleResponseDto> findByEmployeeId(Long employeeId) {
        employeeService.validateEmployeeExists(employeeId);
        return scheduleRepository.findByEmployeeIdFetchEmployee(employeeId)
                .stream()
                .map(this::buildResponseDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ScheduleResponseDto> findByDayOfWeek(DayOfWeek dayOfWeek) {
        return scheduleRepository.findByDayOfWeekFetchEmployee(dayOfWeek)
                .stream()
                .map(this::buildResponseDto)
                .toList();
    }

    // ──────────── UPDATE ────────────

    @Transactional
    public ScheduleResponseDto update(Long id, ScheduleRequestDto dto) {
        Schedule schedule = getScheduleById(id);

        if (!schedule.getEmployee().getId().equals(dto.getEmployeeId())) {
            throw new EntityConflictException("No se puede reasignar un horario a otro empleado/a");
        }

        employeeService.validateEmployeeIsActive(schedule.getEmployee());

        validateNoOverlap(schedule.getEmployee().getId(), dto.getDayOfWeek(),
                dto.getStartTime(), dto.getEndTime(), schedule.getId());

        schedule.setDayOfWeek(dto.getDayOfWeek());
        schedule.setStartTime(dto.getStartTime());
        schedule.setEndTime(dto.getEndTime());

        return buildResponseDto(schedule);
    }

    // ──────────── DELETE ────────────

    @Transactional
    public void delete(Long id) {
        Schedule schedule = getScheduleById(id);
        scheduleRepository.delete(schedule);
    }

    // ──────────── PRIVATES ────────────

    private Schedule getScheduleById(Long id) {
        return scheduleRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "No se encontró el horario con ID: " + id));
    }

    private void validateNoOverlap(Long employeeId, DayOfWeek day,
                                   LocalTime start, LocalTime end, Long excludeScheduleId) {
        if (scheduleRepository.existsOverlappingSchedule(
                employeeId, day, EmployeeStatus.ACTIVE, start, end, excludeScheduleId)) {
            String dia = day.getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.forLanguageTag("es"));
            throw new EntityConflictException(
                    "El horario se superpone con otro existente el día " + dia);
        }
    }

    // Mapper
    private ScheduleResponseDto buildResponseDto(Schedule schedule) {
        Employee employee = schedule.getEmployee();
        return ScheduleResponseDto.builder()
                .id(schedule.getId())
                .employeeId(employee.getId())
                .employeeFirstName(employee.getFirstName())
                .employeeLastName(employee.getLastName())
                .dayOfWeek(schedule.getDayOfWeek())
                .startTime(schedule.getStartTime())
                .endTime(schedule.getEndTime())
                .build();
    }
}