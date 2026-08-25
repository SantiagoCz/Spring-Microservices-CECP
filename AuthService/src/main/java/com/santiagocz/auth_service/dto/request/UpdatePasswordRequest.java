package com.santiagocz.auth_service.dto.request;

import com.santiagocz.auth_service.validation.ValidPassword;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.ToString;

@Data
@ToString(exclude = {"currentPassword", "newPassword"})
public class UpdatePasswordRequest {

    @NotBlank(message = "La contraseña actual es obligatoria")
    private String currentPassword;

    @ValidPassword
    private String newPassword;
}
