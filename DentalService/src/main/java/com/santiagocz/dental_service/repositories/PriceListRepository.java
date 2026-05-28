package com.santiagocz.dental_service.repositories;

import com.santiagocz.dental_service.domain.entities.PriceList;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PriceListRepository extends JpaRepository<PriceList, Long> {

    Optional<PriceList> findByName(String name);

}