package com.santiagocz.medical_coverage_service.domain.entities;

import com.santiagocz.medical_coverage_service.domain.enums.Delegation;
import com.santiagocz.medical_coverage_service.domain.enums.Status;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "payment")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDate date;

    @Column(nullable = false)
    private Double amount;

    @Column
    private Integer discount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    @Column
    private Double discountAmount;

    @OneToOne
    @JoinColumn(name = "medical_order_id", unique = true)
    private MedicalOrder medicalOrder;

    @Column(nullable = false)
    private Long affiliateId;

    @Column(nullable = false)
    private Long creatorId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Delegation delegation;

    // TODO: auditoría
}