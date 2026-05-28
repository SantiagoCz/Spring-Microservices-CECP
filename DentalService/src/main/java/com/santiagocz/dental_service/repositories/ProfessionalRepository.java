package com.santiagocz.dental_service.repositories;

import com.santiagocz.dental_service.domain.entities.Professional;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProfessionalRepository extends JpaRepository<Professional, Long> {

    List<Professional> findAllByOrderByNameAsc();

}