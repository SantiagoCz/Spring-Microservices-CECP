package com.santiagocz.auth_service.dto.response;

import com.santiagocz.auth_service.domain.enums.HierarchyRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

    private String username;

    private HierarchyRole hierarchyRole;

    private TokenResponse token;

    private PersonResponse person;
}