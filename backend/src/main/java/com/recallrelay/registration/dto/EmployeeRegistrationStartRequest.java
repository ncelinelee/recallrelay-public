package com.recallrelay.registration.dto;

import com.recallrelay.employee.EmployeeDepartment;
import com.recallrelay.employee.EmployeeRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record EmployeeRegistrationStartRequest(
        @NotBlank String organizationCode,
        @NotBlank @Size(max=80) String employeeId,
        @NotBlank @Size(max=120) String employeeName,
        @NotBlank @Email @Size(max=150) String companyEmail,
        @NotBlank @Size(max=40) String phone,
        Long dealershipId,
        @NotNull EmployeeDepartment department,
        @NotNull EmployeeRole requestedRole,
        @NotBlank @Size(min=8,max=100) String password
) {}
