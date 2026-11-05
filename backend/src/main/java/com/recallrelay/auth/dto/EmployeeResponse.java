package com.recallrelay.auth.dto;

import com.recallrelay.employee.Employee;
import com.recallrelay.employee.EmployeeRole;

public record EmployeeResponse(
        Long id,
        Long organizationId,
        Long dealershipId,
        String employeeLoginId,
        String employeeName,
        String companyEmail,
        EmployeeRole role,
        boolean active,
        boolean mustChangePassword
) {
    public static EmployeeResponse from(Employee employee) {
        return new EmployeeResponse(
                employee.getId(),
                employee.getOrganizationId(),
                employee.getDealershipId(),
                employee.getEmployeeLoginId(),
                employee.getEmployeeName(),
                employee.getCompanyEmail(),
                employee.getRole(),
                employee.isActive(),
                employee.isMustChangePassword()
        );
    }
}
