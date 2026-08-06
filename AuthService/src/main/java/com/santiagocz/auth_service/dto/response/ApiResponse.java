package com.santiagocz.auth_service.dto.response;

public record ApiResponse(int status, String message, Object data) {
}