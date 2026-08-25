package com.santiagocz.auth_service.dto.request;

import com.santiagocz.auth_service.domain.enums.HierarchyRole;
import com.santiagocz.auth_service.validation.ValidPassword;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = "password")
public class RegisterRequest {

    @NotNull(message = "Los datos de la persona son obligatorios")
    @Valid
    private PersonRequest person;

    @ValidPassword
    private String password;

    @NotNull(message = "El rol jerárquico es obligatorio")
    private HierarchyRole hierarchyRole;
}