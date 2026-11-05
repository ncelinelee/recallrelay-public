package com.recallrelay.employee;

import com.recallrelay.common.ApiException;
import com.recallrelay.dealership.Dealership;
import com.recallrelay.dealership.DealershipRepository;
import com.recallrelay.employee.dto.EmployeeCreateRequest;
import com.recallrelay.employee.dto.EmployeeManagementResponse;
import com.recallrelay.employee.dto.EmployeeUpdateRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final DealershipRepository dealershipRepository;
    private final PasswordEncoder passwordEncoder;

    public EmployeeService(
            EmployeeRepository employeeRepository,
            DealershipRepository dealershipRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.employeeRepository = employeeRepository;
        this.dealershipRepository = dealershipRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public List<EmployeeManagementResponse> list(Jwt jwt, String q) {
        Actor actor = Actor.from(jwt);
        requireManager(actor);

        List<Employee> employees;
        if (actor.role() == EmployeeRole.HEAD_OFFICE_ADMIN) {
            employees = employeeRepository.findAllByOrganizationIdAndDeletedAtIsNullOrderByEmployeeLoginIdAsc(actor.organizationId());
        } else if (actor.role() == EmployeeRole.HEAD_OFFICE_MANAGER) {
            employees = employeeRepository.findAllByOrganizationIdAndDeletedAtIsNullOrderByEmployeeLoginIdAsc(actor.organizationId())
                    .stream()
                    .filter(employee -> employee.getDepartment() == EmployeeDepartment.HEAD_OFFICE)
                    .toList();
        } else {
            requireDealership(actor);
            employees = employeeRepository.findAllByOrganizationIdAndDealershipIdAndDeletedAtIsNullOrderByEmployeeLoginIdAsc(actor.organizationId(), actor.dealershipId());
        }

        String normalized = q == null ? "" : q.trim().toLowerCase(Locale.ROOT);
        return employees.stream()
                .filter(employee -> normalized.isEmpty() || matches(employee, normalized))
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public EmployeeManagementResponse create(Jwt jwt, EmployeeCreateRequest request) {
        Actor actor = Actor.from(jwt);
        requireManager(actor);

        String loginId = request.employeeLoginId().trim();
        String employeeName = request.employeeName().trim();
        String email = request.companyEmail().trim().toLowerCase(Locale.ROOT);
        Long dealershipId = resolveWritableDealership(actor, request.dealershipId(), request.role());

        if (employeeRepository.existsByOrganizationIdAndEmployeeLoginIdIgnoreCase(
                actor.organizationId(), loginId)) {
            throw conflict("Employee ID already exists in this Organization.");
        }
        if (employeeRepository.existsByOrganizationIdAndCompanyEmailIgnoreCase(
                actor.organizationId(), email)) {
            throw conflict("Company email already exists in this Organization.");
        }

        validateRoleForActor(actor, request.role());
        validateDealership(actor.organizationId(), dealershipId, request.role());

        Employee employee = Employee.create(
                actor.organizationId(),
                dealershipId,
                loginId,
                employeeName,
                email,
                passwordEncoder.encode(request.temporaryPassword()),
                request.role()
        );
        return toResponse(employeeRepository.save(employee));
    }

    @Transactional
    public EmployeeManagementResponse update(Jwt jwt, Long employeeId, EmployeeUpdateRequest request) {
        Actor actor = Actor.from(jwt);
        requireManager(actor);

        Employee target = employeeRepository.findByIdAndDeletedAtIsNull(employeeId)
                .orElseThrow(() -> notFound("Employee not found."));
        enforceTargetScope(actor, target);
        validateRoleForActor(actor, request.role());

        if (actor.employeeId().equals(target.getId()) && List.of(EmployeeRole.SERVICE_MANAGER, EmployeeRole.HEAD_OFFICE_MANAGER).contains(actor.role())) {
            if (request.role() != actor.role() || !request.active()) {
                throw forbidden("Manager cannot change their own manager role or deactivate their own account.");
            }
        }

        Long dealershipId = resolveWritableDealership(actor, request.dealershipId(), request.role());
        validateDealership(actor.organizationId(), dealershipId, request.role());

        String email = request.companyEmail().trim().toLowerCase(Locale.ROOT);
        if (employeeRepository.existsByOrganizationIdAndCompanyEmailIgnoreCaseAndIdNot(
                actor.organizationId(), email, target.getId())) {
            throw conflict("Company email already exists in this Organization.");
        }

        target.updateProfile(
                request.employeeName().trim(),
                email,
                dealershipId,
                request.role(),
                request.active()
        );
        return toResponse(target);
    }


    private EmployeeManagementResponse toResponse(Employee employee) {
        String dealershipName = null;
        if (employee.getDealershipId() != null) {
            dealershipName = dealershipRepository.findById(employee.getDealershipId())
                    .map(Dealership::getName)
                    .orElse(null);
        }
        return EmployeeManagementResponse.from(employee, dealershipName);
    }

    private boolean matches(Employee employee, String q) {
        return employee.getEmployeeLoginId().toLowerCase(Locale.ROOT).contains(q)
                || employee.getEmployeeName().toLowerCase(Locale.ROOT).contains(q)
                || employee.getCompanyEmail().toLowerCase(Locale.ROOT).contains(q)
                || employee.getRole().name().toLowerCase(Locale.ROOT).contains(q);
    }

    private void requireManager(Actor actor) {
        if (actor.role() != EmployeeRole.HEAD_OFFICE_ADMIN && actor.role() != EmployeeRole.HEAD_OFFICE_MANAGER && actor.role() != EmployeeRole.SERVICE_MANAGER) {
            throw forbidden("Employee Management is available only to Head Office Admin, Head Office Manager, or Service Manager.");
        }
    }

    private void requireDealership(Actor actor) {
        if (actor.dealershipId() == null) {
            throw forbidden("Assigned dealership is required for this operation.");
        }
    }

    private void enforceTargetScope(Actor actor, Employee target) {
        if (!actor.organizationId().equals(target.getOrganizationId())) {
            throw forbidden("Employee is outside your Organization scope.");
        }
        if (actor.role() == EmployeeRole.SERVICE_MANAGER) {
            requireDealership(actor);
            if (!actor.dealershipId().equals(target.getDealershipId())) throw forbidden("Service Manager can manage only employees in the assigned dealership.");
            if (target.getDepartment() == EmployeeDepartment.HEAD_OFFICE) throw forbidden("Service Manager cannot manage Head Office accounts.");
        } else if (actor.role() == EmployeeRole.HEAD_OFFICE_MANAGER) {
            if (target.getDepartment() != EmployeeDepartment.HEAD_OFFICE || target.getRole() == EmployeeRole.HEAD_OFFICE_ADMIN) {
                throw forbidden("Head Office Manager can manage Head Office staff but not Head Office Admin.");
            }
        }
    }

    private void validateRoleForActor(Actor actor, EmployeeRole requestedRole) {
        if (actor.role() == EmployeeRole.SERVICE_MANAGER && List.of(EmployeeRole.HEAD_OFFICE_ADMIN, EmployeeRole.HEAD_OFFICE_MANAGER, EmployeeRole.HEAD_OFFICE_STAFF, EmployeeRole.CALL_CENTER_AGENT, EmployeeRole.SERVICE_MANAGER).contains(requestedRole)) {
            throw forbidden("Service Manager cannot assign Head Office roles.");
        }
        if (actor.role() == EmployeeRole.HEAD_OFFICE_MANAGER && !List.of(EmployeeRole.HEAD_OFFICE_STAFF, EmployeeRole.CALL_CENTER_AGENT).contains(requestedRole)) {
            throw forbidden("Head Office Manager can assign only Head Office Staff or Call Center Agent roles.");
        }
    }

    private Long resolveWritableDealership(Actor actor, Long requestedDealershipId, EmployeeRole requestedRole) {
        if (actor.role() == EmployeeRole.SERVICE_MANAGER) {
            requireDealership(actor);
            return actor.dealershipId();
        }
        if (actor.role() == EmployeeRole.HEAD_OFFICE_MANAGER || List.of(EmployeeRole.HEAD_OFFICE_ADMIN, EmployeeRole.HEAD_OFFICE_MANAGER, EmployeeRole.HEAD_OFFICE_STAFF, EmployeeRole.CALL_CENTER_AGENT).contains(requestedRole)) {
            return null;
        }
        if (requestedDealershipId == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Dealership is required for dealership roles.");
        }
        return requestedDealershipId;
    }

    private void validateDealership(Long organizationId, Long dealershipId, EmployeeRole role) {
        if (List.of(EmployeeRole.HEAD_OFFICE_ADMIN, EmployeeRole.HEAD_OFFICE_MANAGER, EmployeeRole.HEAD_OFFICE_STAFF, EmployeeRole.CALL_CENTER_AGENT).contains(role)) {
            return;
        }
        if (dealershipId == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Dealership is required for dealership roles.");
        }
        dealershipRepository.findByIdAndOrganizationId(dealershipId, organizationId)
                .filter(Dealership::isActive)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.BAD_REQUEST,
                        "VALIDATION_ERROR",
                        "Dealership is invalid or inactive."
                ));
    }

    private ApiException forbidden(String message) {
        return new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", message);
    }

    private ApiException conflict(String message) {
        return new ApiException(HttpStatus.CONFLICT, "CONFLICT", message);
    }

    private ApiException notFound(String message) {
        return new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", message);
    }

    private record Actor(Long employeeId, Long organizationId, Long dealershipId, EmployeeRole role) {
        static Actor from(Jwt jwt) {
            Number employeeId = jwt.getClaim("employeeId");
            Number organizationId = jwt.getClaim("organizationId");
            Number dealershipId = jwt.getClaim("dealershipId");
            String role = jwt.getClaimAsString("role");

            if (employeeId == null || organizationId == null || role == null) {
                throw new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "JWT scope claims are incomplete.");
            }

            return new Actor(
                    employeeId.longValue(),
                    organizationId.longValue(),
                    dealershipId == null ? null : dealershipId.longValue(),
                    EmployeeRole.valueOf(role)
            );
        }
    }
}
