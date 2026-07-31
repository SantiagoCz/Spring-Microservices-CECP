package com.santiagocz.auth_service.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PersonResponse {

    private Long id;

    private String dni;

    private String firstName;

    private String lastName;

    private String phoneNumber;

    private LocalDate birthDate;
}