package com.santiagocz.dental_service.repositories;

import com.santiagocz.dental_service.domain.entities.CommissionPrice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface CommissionPriceRepository extends JpaRepository<CommissionPrice, Long> {

    List<CommissionPrice> findByCodeIdAndPriceListId(Long codeId, Long priceListId);

    @Query("""
            SELECT cp FROM CommissionPrice cp
            WHERE cp.code.id = :codeId
              AND cp.priceList.id = :priceListId
              AND cp.validFrom <= :date
              AND (cp.validUntil IS NULL OR cp.validUntil >= :date)
            """)
    Optional<CommissionPrice> findActiveByCodeAndPriceListAndDate(
            @Param("codeId") Long codeId,
            @Param("priceListId") Long priceListId,
            @Param("date") LocalDate date
    );
}