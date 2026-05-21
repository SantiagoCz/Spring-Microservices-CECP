package com.santiagocz.patients_service.domain.entities;

import com.santiagocz.patients_service.domain.enums.Status;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "patients")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Patient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String dni;

    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    private String phoneNumber;

    @Column(nullable = false)
    private LocalDate birthDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    // TODO: Auditoría
//    private Long creatorId;
//    private LocalDateTime creationDate;
//    private Long modifierId;
//    private LocalDateTime modificationDate;
//    private Long deleterId;
//    private LocalDateTime deletionDate;
}