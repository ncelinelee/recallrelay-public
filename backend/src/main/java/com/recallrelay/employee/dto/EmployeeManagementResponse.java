package com.recallrelay.employee.dto;

import com.recallrelay.employee.Employee;
import com.recallrelay.employee.EmployeeRole;

public record EmployeeManagementResponse(
        Long id,
        Long organizationId,
        Long dealershipId,
        String dealershipName,
        String employeeLoginId,
        String employeeName,
        String companyEmail,
        EmployeeRole role,
        boolean active
) {
    public static EmployeeManagementResponse from(Employee employee, String dealershipName) {
        return new EmployeeManagementResponse(
                employee.getId(),
                employee.getOrganizationId(),
                employee.getDealershipId(),
                dealershipName,
                employee.getEmployeeLoginId(),
                employee.getEmployeeName(),
                employee.getCompanyEmail(),
                employee.getRole(),
                employee.isActive()
        );
    }
}
