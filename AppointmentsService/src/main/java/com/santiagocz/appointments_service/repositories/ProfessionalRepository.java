package com.santiagocz.appointments_service.repositories;

import com.santiagocz.appointments_service.domain.entities.Professional;
import com.santiagocz.appointments_service.domain.enums.Specialty;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProfessionalRepository extends JpaRepository<Professional, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Professional p WHERE p.id = :id")
    Optional<Professional> findByIdWithLock(@Param("id") Long id);

    Optional<Professional> findByDni(String dni);

    boolean existsByDni(String dni);

    boolean existsByLicenseNumberAndSpecialty(String licenseNumber, Specialty specialty);

    Page<Professional> findAllByOrderByLastNameAsc(Pageable pageable);

    Page<Professional> findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCase(
            String firstName, String lastName, Pageable pageable);
}