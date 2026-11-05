package com.recallrelay.employee.dto;

import com.recallrelay.employee.EmployeeRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record EmployeeCreateRequest(
        @NotBlank(message = "Employee ID is required.")
        @Size(max = 80, message = "Employee ID must be 80 characters or fewer.")
        String employeeLoginId,

        @NotBlank(message = "Employee name is required.")
        @Size(max = 120, message = "Employee name must be 120 characters or fewer.")
        String employeeName,

        @NotBlank(message = "Company email is required.")
        @Email(message = "Company email format is invalid.")
        @Size(max = 150, message = "Company email must be 150 characters or fewer.")
        String companyEmail,

        Long dealershipId,

        @NotNull(message = "Role is required.")
        EmployeeRole role,

        @NotBlank(message = "Temporary password is required.")
        @Size(min = 8, max = 100, message = "Temporary password must be between 8 and 100 characters.")
        String temporaryPassword
) {
}
