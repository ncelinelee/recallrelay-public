package com.recallrelay.auth.dto;

public record LoginResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        boolean mustChangePassword,
        EmployeeResponse employee
) {
}
