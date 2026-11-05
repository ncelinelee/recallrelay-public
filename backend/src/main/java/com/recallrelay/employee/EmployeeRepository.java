package com.recallrelay.employee;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {
    Optional<Employee> findByOrganizationIdAndEmployeeLoginIdAndDeletedAtIsNull(Long organizationId, String employeeLoginId);

    Optional<Employee> findByIdAndDeletedAtIsNull(Long id);

    Optional<Employee> findByOrganizationIdAndCompanyEmailIgnoreCaseAndDeletedAtIsNull(Long organizationId, String companyEmail);

    List<Employee> findAllByOrganizationIdAndDeletedAtIsNullOrderByEmployeeLoginIdAsc(Long organizationId);

    List<Employee> findAllByOrganizationIdAndDealershipIdAndDeletedAtIsNullOrderByEmployeeLoginIdAsc(
            Long organizationId,
            Long dealershipId
    );

    boolean existsByOrganizationIdAndEmployeeLoginIdIgnoreCase(Long organizationId, String employeeLoginId);

    boolean existsByOrganizationIdAndCompanyEmailIgnoreCase(Long organizationId, String companyEmail);

    boolean existsByOrganizationIdAndCompanyEmailIgnoreCaseAndIdNot(
            Long organizationId,
            String companyEmail,
            Long id
    );
}
