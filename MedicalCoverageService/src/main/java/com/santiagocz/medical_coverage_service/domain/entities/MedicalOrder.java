package com.santiagocz.medical_coverage_service.domain.entities;

import com.santiagocz.medical_coverage_service.domain.enums.MedicalOrderType;
import com.santiagocz.medical_coverage_service.domain.enums.Status;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "medical_order")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MedicalOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long number;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MedicalOrderType medicalOrderType;

    @OneToOne(mappedBy = "medicalOrder")
    private Payment payment;

    // TODO: auditoría
}