package com.santiagocz.auth_service.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {

    @NotNull(message = "Los datos de la persona son obligatorios")
    @Valid
    private PersonRequest person;

    @NotBlank(message = "La contraseña es obligatoria")
    @Size(min = 4, message = "La contraseña debe tener al menos 4 caracteres") //TODO: despues modificaremos el patrón de la contraseña
    private String password;

    @NotNull(message = "El rol jerárquico es obligatorio")
    private String hierarchyRole;
}