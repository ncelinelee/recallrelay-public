package com.recallrelay.registration.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record EmployeeRegistrationVerifyRequest(
        @NotBlank String organizationCode,
        @NotBlank String employeeId,
        @NotBlank @Pattern(regexp="\\d{6}", message="Verification code must be 6 digits.") String verificationCode
) {}
