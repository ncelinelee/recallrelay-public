package com.recallrelay.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank @Size(max = 50) String organizationCode,
        @NotBlank @Size(max = 80) String employeeId,
        @NotBlank String password
) {
}
