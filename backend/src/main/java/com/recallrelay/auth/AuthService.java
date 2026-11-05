package com.recallrelay.auth;

import com.recallrelay.auth.dto.EmployeeResponse;
import com.recallrelay.auth.dto.LoginRequest;
import com.recallrelay.auth.dto.LoginResponse;
import com.recallrelay.auth.dto.PasswordChangeRequest;
import com.recallrelay.common.ApiException;
import com.recallrelay.employee.Employee;
import com.recallrelay.employee.EmployeeRepository;
import com.recallrelay.organization.OrganizationRepository;
import com.recallrelay.security.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final OrganizationRepository organizationRepository;
    private final EmployeeRepository employeeRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(
            OrganizationRepository organizationRepository,
            EmployeeRepository employeeRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService
    ) {
        this.organizationRepository = organizationRepository;
        this.employeeRepository = employeeRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        String organizationCode = request.organizationCode().trim();
        String employeeId = request.employeeId().trim();

        var organization = organizationRepository.findByOrganizationCode(organizationCode)
                .orElseThrow(this::invalidCredentials);

        Employee employee = employeeRepository
                .findByOrganizationIdAndEmployeeLoginIdAndDeletedAtIsNull(organization.getId(), employeeId)
                .orElseThrow(this::invalidCredentials);

        if (!passwordEncoder.matches(request.password(), employee.getPasswordHash())) {
            throw invalidCredentials();
        }

        if (!employee.isActive()) {
            throw new ApiException(
                    HttpStatus.FORBIDDEN,
                    "FORBIDDEN",
                    "Inactive account cannot access RecallRelay."
            );
        }

        JwtService.Token token = jwtService.issue(employee);

        return new LoginResponse(
                token.value(),
                "Bearer",
                token.expiresIn(),
                employee.isMustChangePassword(),
                EmployeeResponse.from(employee)
        );
    }

    @Transactional
    public void changePassword(Jwt jwt, PasswordChangeRequest request) {
        if (jwt == null || jwt.getSubject() == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Authentication is required.");
        }

        Long employeeId;
        try {
            employeeId = Long.valueOf(jwt.getSubject());
        } catch (NumberFormatException ex) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Authentication subject is invalid.");
        }

        Employee employee = employeeRepository.findByIdAndDeletedAtIsNull(employeeId)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Employee account is unavailable."));

        if (!employee.isActive()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", "Inactive account cannot change password.");
        }

        if (!passwordEncoder.matches(request.currentPassword(), employee.getPasswordHash())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_CURRENT_PASSWORD", "Current password is incorrect.");
        }

        if (passwordEncoder.matches(request.newPassword(), employee.getPasswordHash())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "PASSWORD_REUSE", "New password must be different from the current password.");
        }

        employee.changePassword(passwordEncoder.encode(request.newPassword()));
    }

    private ApiException invalidCredentials() {
        return new ApiException(
                HttpStatus.UNAUTHORIZED,
                "UNAUTHORIZED",
                "Organization Code, Employee ID, or Password is invalid."
        );
    }
}
