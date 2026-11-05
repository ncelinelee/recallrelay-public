package com.recallrelay.registration;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface RegistrationRequestRepository extends JpaRepository<RegistrationRequest, Long> {
    Optional<RegistrationRequest> findFirstByOrganizationIdAndEmployeeLoginIdIgnoreCaseAndStatusOrderByIdDesc(
            Long organizationId, String employeeLoginId, RegistrationStatus status);
    boolean existsByOrganizationIdAndEmployeeLoginIdIgnoreCaseAndStatusIn(Long organizationId, String employeeLoginId, List<RegistrationStatus> statuses);
    boolean existsByOrganizationIdAndCompanyEmailIgnoreCaseAndStatusIn(Long organizationId, String companyEmail, List<RegistrationStatus> statuses);
    List<RegistrationRequest> findAllByOrganizationIdAndStatusOrderByCreatedAtAsc(Long organizationId, RegistrationStatus status);
}
