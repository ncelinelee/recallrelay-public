package com.recallrelay.registration.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record PasswordResetRequest(@NotBlank String organizationCode, @NotBlank @Email String companyEmail) {}
