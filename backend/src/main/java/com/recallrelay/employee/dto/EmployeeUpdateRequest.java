package com.recallrelay.employee.dto;

import com.recallrelay.employee.EmployeeRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record EmployeeUpdateRequest(
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

        @NotNull(message = "Active status is required.")
        Boolean active
) {
}
