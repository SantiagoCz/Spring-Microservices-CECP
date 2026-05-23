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

    @Column(name = "dni", nullable = false, unique = true)
    private String dni;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Column(name = "phone_number")
    private String phoneNumber;

    @Column(name = "birth_date", nullable = false)
    private LocalDate birthDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private Status status;

    // TODO: Auditoría
//    private Long creatorId;
//    private LocalDateTime creationDate;
//    private Long modifierId;
//    private LocalDateTime modificationDate;
//    private Long deleterId;
//    private LocalDateTime deletionDate;
}