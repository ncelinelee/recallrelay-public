package com.recallrelay.registration.dto;

import com.recallrelay.employee.EmployeeDepartment;
import com.recallrelay.employee.EmployeeRole;
import com.recallrelay.registration.RegistrationRequest;
import java.time.LocalDateTime;

public record PendingRegistrationResponse(
        Long id,
        String employeeId,
        String employeeName,
        String companyEmail,
        String phone,
        Long dealershipId,
        EmployeeDepartment department,
        EmployeeRole requestedRole,
        LocalDateTime requestedAt
) {
    public static PendingRegistrationResponse from(RegistrationRequest r){
        return new PendingRegistrationResponse(r.getId(), r.getEmployeeLoginId(), r.getEmployeeName(), r.getCompanyEmail(), r.getPhone(), r.getDealershipId(), r.getDepartment(), r.getRequestedRole(), r.getCreatedAt());
    }
}
