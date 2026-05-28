package com.santiagocz.dental_service.domain.entities;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "codes")
@Getter
@Setter
@NoArgsConstructor
public class Code {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Integer number;

    @Column(nullable = false)
    private String description;

}