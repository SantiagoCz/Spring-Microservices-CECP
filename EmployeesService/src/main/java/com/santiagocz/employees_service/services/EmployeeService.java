package com.santiagocz.employees_service.services;

import com.santiagocz.employees_service.domain.entities.Employee;
import com.santiagocz.employees_service.domain.enums.EmployeeRole;
import com.santiagocz.employees_service.domain.enums.EmployeeStatus;
import com.santiagocz.employees_service.dto.employee.EmployeeRequestDto;
import com.santiagocz.employees_service.dto.employee.EmployeeResponseDto;
import com.santiagocz.employees_service.exceptions.EntityConflictException;
import com.santiagocz.employees_service.exceptions.EntityNotFoundException;
import com.santiagocz.employees_service.repositories.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EmployeeService {

    private final EmployeeRepository employeeRepository;

    // ──────────── CREATE ────────────

    @Transactional
    public EmployeeResponseDto create(EmployeeRequestDto dto) {
        validateDniNotInUse(dto.getDni());

        Employee employee = buildEmployee(dto);
        employee.setStatus(EmployeeStatus.ACTIVE);
        return buildResponseDto(employeeRepository.save(employee));
    }

    // ──────────── READ ────────────

    @Transactional(readOnly = true)
    public List<EmployeeResponseDto> findAll() {
        return employeeRepository.findAll()
                .stream()
                .map(this::buildResponseDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public EmployeeResponseDto findById(Long id) {
        return buildResponseDto(getEmployeeById(id));
    }

    @Transactional(readOnly = true)
    public EmployeeResponseDto findByDni(String dni) {
        return buildResponseDto(getEmployeeByDni(dni));
    }

    @Transactional(readOnly = true)
    public List<EmployeeResponseDto> findByStatus(EmployeeStatus status) {
        return employeeRepository.findByStatus(status)
                .stream()
                .map(this::buildResponseDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<EmployeeResponseDto> findByRole(EmployeeRole role) {
        return employeeRepository.findByRole(role)
                .stream()
                .map(this::buildResponseDto)
                .toList();
    }

    // ──────────── UPDATE ────────────

    @Transactional
    public EmployeeResponseDto update(Long id, EmployeeRequestDto dto) {
        Employee employee = getEmployeeById(id);

        if (!employee.getDni().equals(dto.getDni())) {
            validateDniNotInUse(dto.getDni());
        }

        employee.setDni(dto.getDni());
        employee.setFirstName(formatWords(dto.getFirstName()));
        employee.setLastName(formatWords(dto.getLastName()));
        employee.setPhone(dto.getPhone());
        employee.setBirthDate(dto.getBirthDate());
        employee.setDelegation(dto.getDelegation());
        employee.setRole(dto.getRole());

        return buildResponseDto(employee);
    }

    // ──────────── STATUS ────────────

    @Transactional
    public void deactivate(Long id) {
        Employee employee = getEmployeeById(id);

        if (employee.getStatus() == EmployeeStatus.INACTIVE) {
            throw new EntityConflictException("Empleado/a ya está inactivo");
        }

        employee.setStatus(EmployeeStatus.INACTIVE);
    }

    @Transactional
    public void activate(Long id) {
        Employee employee = getEmployeeById(id);

        if (employee.getStatus() == EmployeeStatus.ACTIVE) {
            throw new EntityConflictException("Empleado/a ya está activo");
        }

        employee.setStatus(EmployeeStatus.ACTIVE);
    }

    // ──────────── PRIVATES AND AUX METHODS ────────────

    public void validateEmployeeExists(Long id) {
        if (!employeeRepository.existsById(id)) {
            throw new EntityNotFoundException(
                    "No se encontró empleado/a con ID: " + id);
        }
    }

    private void validateDniNotInUse(String dni) {
        if (employeeRepository.existsByDni(dni)) {
            throw new EntityConflictException(
                    "Ya existe empleado/a con el DNI: " + dni);
        }
    }

    public Employee getEmployeeById(Long id) {
        return employeeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "No se encontró empleado/a con ID: " + id));
    }

    public Employee getEmployeeByDni(String dni) {
        return employeeRepository.findByDni(dni)
                .orElseThrow(() -> new EntityNotFoundException(
                        "No se encontró empleado/a con DNI: " + dni));
    }

    public void validateEmployeeIsActive(Employee employee) {
        if (employee.getStatus() != EmployeeStatus.ACTIVE) {
            throw new EntityConflictException("Empleado/a no activo");
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
    private EmployeeResponseDto buildResponseDto(Employee employee) {
        return EmployeeResponseDto.builder()
                .id(employee.getId())
                .dni(employee.getDni())
                .firstName(employee.getFirstName())
                .lastName(employee.getLastName())
                .phone(employee.getPhone())
                .birthDate(employee.getBirthDate())
                .delegation(employee.getDelegation())
                .role(employee.getRole())
                .status(employee.getStatus())
                .build();
    }

    private Employee buildEmployee(EmployeeRequestDto dto) {
        return Employee.builder()
                .dni(dto.getDni())
                .firstName(formatWords(dto.getFirstName()))
                .lastName(formatWords(dto.getLastName()))
                .phone(dto.getPhone())
                .birthDate(dto.getBirthDate())
                .delegation(dto.getDelegation())
                .role(dto.getRole())
                .build();
    }
}