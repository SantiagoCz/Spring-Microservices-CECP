package com.santiagocz.appointments_service.repositories;

import com.santiagocz.appointments_service.domain.entities.Professional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProfessionalRepository extends JpaRepository<Professional, Long> {

    Optional<Professional> findByDni(String dni);

    boolean existsByDni(String dni);

    Page<Professional> findAllByOrderByLastNameAsc(Pageable pageable);

    Page<Professional> findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCase(
            String firstName, String lastName, Pageable pageable);
}