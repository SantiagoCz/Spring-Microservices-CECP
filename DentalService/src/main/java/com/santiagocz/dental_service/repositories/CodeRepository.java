package com.santiagocz.dental_service.repositories;

import com.santiagocz.dental_service.domain.entities.Code;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CodeRepository extends JpaRepository<Code, Long> {

    Optional<Code> findByNumber(Integer number);

}