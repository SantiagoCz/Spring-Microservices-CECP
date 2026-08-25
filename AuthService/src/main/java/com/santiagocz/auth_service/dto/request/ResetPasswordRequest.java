package com.santiagocz.auth_service.dto.request;

import com.santiagocz.auth_service.validation.ValidPassword;
import lombok.Data;
import lombok.ToString;

@Data
@ToString(exclude = "newPassword")
public class ResetPasswordRequest {

    @ValidPassword
    private String newPassword;

}