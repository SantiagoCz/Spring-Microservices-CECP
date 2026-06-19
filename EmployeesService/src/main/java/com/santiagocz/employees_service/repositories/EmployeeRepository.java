package com.santiagocz.employees_service.repositories;

import com.santiagocz.employees_service.domain.entities.Employee;
import com.santiagocz.employees_service.domain.enums.EmployeeRole;
import com.santiagocz.employees_service.domain.enums.EmployeeStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    Optional<Employee> findByDni(String dni);

    List<Employee> findByStatus(EmployeeStatus status);

    List<Employee> findByRole(EmployeeRole role);

    boolean existsByDni(String dni);
}