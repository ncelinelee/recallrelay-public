package com.recallrelay.registration;

import com.recallrelay.employee.EmployeeRole;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ManagerActivationCodeRepository extends JpaRepository<ManagerActivationCode, Long> {
    Optional<ManagerActivationCode> findFirstByOrganizationIdAndEmployeeLoginIdIgnoreCaseAndManagerRoleAndActiveTrueOrderByIdDesc(
            Long organizationId, String employeeLoginId, EmployeeRole managerRole);
}
