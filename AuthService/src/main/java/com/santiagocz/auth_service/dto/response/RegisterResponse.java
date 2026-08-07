package com.santiagocz.auth_service.dto.response;

import com.santiagocz.auth_service.domain.enums.HierarchyRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterResponse {
    private HierarchyRole hierarchyRole;
    private Set<String> subroles;
    private PersonResponse person;
}