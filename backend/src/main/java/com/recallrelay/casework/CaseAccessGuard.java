package com.recallrelay.casework;

import com.recallrelay.common.ApiException;
import com.recallrelay.employee.Employee;
import com.recallrelay.employee.EmployeeRepository;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import java.util.Objects;

/** Shared by case detail and every AI service, before loading customer evidence. */
@Component
public class CaseAccessGuard {
    private final EmployeeRepository employees;
    private final JdbcTemplate jdbc;
    public CaseAccessGuard(EmployeeRepository employees, JdbcTemplate jdbc) {
        this.employees = employees;
        this.jdbc = jdbc;
    }
    public void requireRead(Long caseId) {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof Jwt jwt)) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Authentication required.");
        }
        final long employeeId;
        try { employeeId = Long.parseLong(jwt.getSubject()); }
        catch (RuntimeException e) { throw denied(); }
        // Database state is authoritative: tokens may predate role changes or deactivation.
        Employee employee = employees.findByIdAndDeletedAtIsNull(employeeId)
                .filter(Employee::isActive).orElseThrow(CaseAccessGuard::denied);
        var scopes = jdbc.query("""
                SELECT d.organization_id, rc.dealership_id
                FROM recall_cases rc JOIN dealerships d ON d.id = rc.dealership_id
                WHERE rc.id = ?
                """, (rs, row) -> new Scope(rs.getLong(1), rs.getLong(2)), caseId);
        if (scopes.size() != 1) throw denied();
        Scope scope = scopes.get(0);
        if (!Objects.equals(employee.getOrganizationId(), scope.organizationId())) throw denied();
        boolean organizationWide = switch (employee.getRole()) {
            case HEAD_OFFICE_ADMIN, HEAD_OFFICE_MANAGER, HEAD_OFFICE_STAFF, CALL_CENTER_AGENT -> true;
            default -> false;
        };
        if (!organizationWide && !Objects.equals(employee.getDealershipId(), scope.dealershipId())) throw denied();
    }
    private static ApiException denied() {
        // Same response for absent and inaccessible cases to avoid revealing existence.
        return new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", "Case access denied.");
    }
    private record Scope(Long organizationId, Long dealershipId) {}
}
