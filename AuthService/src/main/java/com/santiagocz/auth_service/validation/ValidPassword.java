package com.santiagocz.auth_service.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = {})
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@NotBlank(message = "La contraseña es obligatoria")
@Size(max = 64, message = "La contraseña no puede superar los 64 caracteres")
@Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,}$",
        message = "La contraseña debe tener como mínimo 8 caracteres, con al menos una mayúscula, una minúscula y un número"
)
public @interface ValidPassword {
    String message() default "Contraseña inválida";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}