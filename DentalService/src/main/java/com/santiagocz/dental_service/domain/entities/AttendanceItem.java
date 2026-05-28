package com.santiagocz.dental_service.domain.entities;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "attendance_items")
@Getter
@Setter
@NoArgsConstructor
public class AttendanceItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "attendance_id", nullable = false)
    private Attendance attendance;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "code_id", nullable = false)
    private Code code;

    private String toothSurface;

}