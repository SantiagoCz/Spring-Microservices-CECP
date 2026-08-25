package com.santiagocz.medical_coverage_service.domain.entities;

import com.santiagocz.common.delegation.Delegation;
import com.santiagocz.common.persistence.Auditable;
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
@EqualsAndHashCode(callSuper = false)
public class Payment extends Auditable {

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

    @OneToOne(cascade = CascadeType.PERSIST)
    @JoinColumn(name = "medical_order_id", unique = true)
    private MedicalOrder medicalOrder;

    @Column(nullable = false)
    private Long affiliateId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Delegation delegation;

}