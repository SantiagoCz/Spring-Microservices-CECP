package com.santiagocz.common.auth;

import com.santiagocz.common.delegation.Delegation;

public record AuthenticatedUser(Long id, String fullName, Delegation delegation) {
}