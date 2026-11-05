package com.recallrelay.casework;

import com.recallrelay.common.ApiException;
import com.recallrelay.employee.*;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.embedded.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CaseAccessGuardTest {
    private EmbeddedDatabase db;
    private EmployeeRepository employees;
    private CaseAccessGuard guard;
    @BeforeEach void setup() {
        db = new EmbeddedDatabaseBuilder().generateUniqueName(true).setType(EmbeddedDatabaseType.H2).build();
        var jdbc = new JdbcTemplate(db);
        jdbc.execute("CREATE TABLE dealerships(id BIGINT, organization_id BIGINT)");
        jdbc.execute("CREATE TABLE recall_cases(id BIGINT, dealership_id BIGINT)");
        jdbc.execute("INSERT INTO dealerships VALUES(10,1),(20,1),(30,2)");
        jdbc.execute("INSERT INTO recall_cases VALUES(100,10),(200,20),(300,30)");
        employees = mock(EmployeeRepository.class);
        guard = new CaseAccessGuard(employees, jdbc);
        // Deliberately overprivileged, stale claims must have no effect on DB-authoritative scope.
        var jwt = Jwt.withTokenValue("test").header("alg","HS256").subject("7")
                .claim("role","HEAD_OFFICE_ADMIN").claim("organizationId",2).build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt, java.util.List.of()));
        actor(EmployeeRole.SERVICE_ADVISOR, 1L, 10L, true);
    }
    private void actor(EmployeeRole role, Long organization, Long dealership, boolean active) {
        Employee e = Employee.create(organization,dealership,"demo","Demo","demo@example.invalid","unused",role);
        e.updateProfile("Demo","demo@example.invalid",dealership,role,active);
        when(employees.findByIdAndDeletedAtIsNull(7L)).thenReturn(Optional.of(e));
    }
    @AfterEach void cleanup() { SecurityContextHolder.clearContext(); db.shutdown(); }
    @Test void assignedDealershipAllowed() { assertDoesNotThrow(() -> guard.requireRead(100L)); }
    @Test void otherDealershipDeniedDespiteAdminClaim() { assertThrows(ApiException.class, () -> guard.requireRead(200L)); }
    @Test void otherOrganizationDenied() { assertThrows(ApiException.class, () -> guard.requireRead(300L)); }
    @Test void headOfficeCanReadOwnOrganization() { actor(EmployeeRole.HEAD_OFFICE_ADMIN,1L,null,true); assertDoesNotThrow(() -> guard.requireRead(200L)); }
    @Test void headOfficeCannotCrossOrganization() { actor(EmployeeRole.HEAD_OFFICE_ADMIN,1L,null,true); assertThrows(ApiException.class, () -> guard.requireRead(300L)); }
    @Test void inactiveEmployeeDenied() { actor(EmployeeRole.SERVICE_ADVISOR,1L,10L,false); assertThrows(ApiException.class, () -> guard.requireRead(100L)); }
    @Test void deletedEmployeeDenied() { when(employees.findByIdAndDeletedAtIsNull(7L)).thenReturn(Optional.empty()); assertThrows(ApiException.class, () -> guard.requireRead(100L)); }
    @Test void missingDealershipDenied() { actor(EmployeeRole.SERVICE_ADVISOR,1L,null,true); assertThrows(ApiException.class, () -> guard.requireRead(100L)); }
    @Test void anonymousDeniedBeforeDatabaseRead() { SecurityContextHolder.clearContext(); assertThrows(ApiException.class, () -> guard.requireRead(100L)); verifyNoInteractions(employees); }
    @Test void missingCaseDoesNotRevealExistence() { assertThrows(ApiException.class, () -> guard.requireRead(999L)); }
    @Test void detailServiceStopsBeforeCustomerDataRead() {
        var jdbc = mock(JdbcTemplate.class);
        var deny = mock(CaseAccessGuard.class);
        doThrow(new ApiException(org.springframework.http.HttpStatus.FORBIDDEN,"FORBIDDEN","Denied")).when(deny).requireRead(300L);
        assertThrows(ApiException.class, () -> new RecallCaseQueryService(jdbc,deny).getCaseDetail(300L));
        verifyNoInteractions(jdbc);
    }
}
