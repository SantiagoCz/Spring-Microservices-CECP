package com.santiagocz.auth_service.dto.response;

import com.santiagocz.auth_service.domain.enums.HierarchyRole;
import com.santiagocz.common.delegation.Delegation;
import lombok.Builder;
import lombok.Data;

import java.util.Set;

@Data
@Builder
public class UserResponse {

    private Long id;
    private String username;
    private HierarchyRole hierarchyRole;
    private Set<String> subroles;
    private Boolean enabled;
    private PersonResponse person;
    private Delegation delegation;

}