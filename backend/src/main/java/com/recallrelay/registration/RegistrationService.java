package com.recallrelay.registration;

import com.recallrelay.common.ApiException;
import com.recallrelay.dealership.Dealership;
import com.recallrelay.dealership.DealershipRepository;
import com.recallrelay.employee.*;
import com.recallrelay.organization.Organization;
import com.recallrelay.organization.OrganizationRepository;
import com.recallrelay.registration.dto.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Locale;

@Service
public class RegistrationService {
    private static final List<RegistrationStatus> ACTIVE_REQUEST_STATUSES = List.of(
            RegistrationStatus.PENDING_EMAIL_VERIFICATION,
            RegistrationStatus.PENDING_MANAGER_APPROVAL
    );
    private static final SecureRandom RANDOM = new SecureRandom();

    private final OrganizationRepository organizationRepository;
    private final DealershipRepository dealershipRepository;
    private final EmployeeRepository employeeRepository;
    private final RegistrationRequestRepository registrationRepository;
    private final ManagerActivationCodeRepository managerCodeRepository;
    private final PasswordResetTokenRepository resetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final NotificationMailService mailService;
    private final boolean devExposeSecrets;
    private final String frontendBaseUrl;

    public RegistrationService(
            OrganizationRepository organizationRepository,
            DealershipRepository dealershipRepository,
            EmployeeRepository employeeRepository,
            RegistrationRequestRepository registrationRepository,
            ManagerActivationCodeRepository managerCodeRepository,
            PasswordResetTokenRepository resetTokenRepository,
            PasswordEncoder passwordEncoder,
            NotificationMailService mailService,
            @Value("${app.mail.dev-expose-secrets:false}") boolean devExposeSecrets,
            @Value("${app.frontend.base-url:http://localhost:5173}") String frontendBaseUrl
    ) {
        this.organizationRepository = organizationRepository;
        this.dealershipRepository = dealershipRepository;
        this.employeeRepository = employeeRepository;
        this.registrationRepository = registrationRepository;
        this.managerCodeRepository = managerCodeRepository;
        this.resetTokenRepository = resetTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.mailService = mailService;
        this.devExposeSecrets = devExposeSecrets;
        this.frontendBaseUrl = frontendBaseUrl.replaceAll("/+$", "");
    }

    @Transactional
    public RegistrationActionResponse startEmployee(EmployeeRegistrationStartRequest request) {
        Organization org = organization(request.organizationCode());
        String loginId = request.employeeId().trim();
        String email = normalizeEmail(request.companyEmail());
        validateCompanyEmail(org, email);
        validateEmployeeRole(request.department(), request.requestedRole());
        Long dealershipId = resolveEmployeeDealership(org.getId(), request.department(), request.dealershipId());
        ensureIdentityAvailable(org.getId(), loginId, email);

        String code = String.format("%06d", RANDOM.nextInt(1_000_000));
        RegistrationRequest registration = RegistrationRequest.employee(
                org.getId(), dealershipId, loginId, request.employeeName().trim(), email, request.phone().trim(),
                request.department(), request.requestedRole(), passwordEncoder.encode(request.password()),
                TokenHash.sha256(code), LocalDateTime.now().plusMinutes(10)
        );
        registrationRepository.save(registration);
        mailService.sendVerificationCode(email, code);
        return new RegistrationActionResponse(
                "Verification code sent to the company email.",
                registration.getStatus().name(),
                devExposeSecrets ? code : null
        );
    }

    @Transactional
    public RegistrationActionResponse verifyEmployee(EmployeeRegistrationVerifyRequest request) {
        Organization org = organization(request.organizationCode());
        RegistrationRequest registration = registrationRepository
                .findFirstByOrganizationIdAndEmployeeLoginIdIgnoreCaseAndStatusOrderByIdDesc(
                        org.getId(), request.employeeId().trim(), RegistrationStatus.PENDING_EMAIL_VERIFICATION)
                .orElseThrow(() -> badRequest("No pending email verification request was found."));

        if (registration.getVerificationExpiresAt() == null || registration.getVerificationExpiresAt().isBefore(LocalDateTime.now())) {
            throw badRequest("Verification code has expired. Request a new code.");
        }
        if (!TokenHash.sha256(request.verificationCode().trim()).equals(registration.getVerificationCodeHash())) {
            throw badRequest("Verification code is invalid.");
        }
        registration.verifyEmail();
        return new RegistrationActionResponse(
                "Company email verified. Manager approval is now pending.",
                registration.getStatus().name(),
                null
        );
    }

    @Transactional
    public RegistrationActionResponse registerManager(ManagerRegistrationRequest request) {
        Organization org = organization(request.organizationCode());
        String loginId = request.employeeId().trim();
        String email = normalizeEmail(request.companyEmail());
        validateCompanyEmail(org, email);
        if (request.managerRole() != EmployeeRole.SERVICE_MANAGER && request.managerRole() != EmployeeRole.HEAD_OFFICE_MANAGER) {
            throw badRequest("Only SERVICE_MANAGER or HEAD_OFFICE_MANAGER can use manager self-registration.");
        }

        Long dealershipId = request.managerRole() == EmployeeRole.SERVICE_MANAGER
                ? requireDealership(org.getId(), request.dealershipId())
                : null;
        ensureIdentityAvailable(org.getId(), loginId, email);

        ManagerActivationCode code = managerCodeRepository
                .findFirstByOrganizationIdAndEmployeeLoginIdIgnoreCaseAndManagerRoleAndActiveTrueOrderByIdDesc(
                        org.getId(), loginId, request.managerRole())
                .orElseThrow(() -> forbidden("Manager activation credentials are invalid."));
        if (!code.isUsable() || !TokenHash.sha256(request.oneTimeCode().trim()).equals(code.getCodeHash())) {
            throw forbidden("Manager activation credentials are invalid or expired.");
        }
        if (request.managerRole() == EmployeeRole.SERVICE_MANAGER && !dealershipId.equals(code.getDealershipId())) {
            throw forbidden("Dealership Number does not match the company-issued manager code.");
        }

        Employee employee = Employee.createSelfRegistered(
                org.getId(), dealershipId, loginId, request.employeeName().trim(), email, request.phone().trim(),
                request.managerRole() == EmployeeRole.SERVICE_MANAGER ? EmployeeDepartment.SERVICE : EmployeeDepartment.HEAD_OFFICE,
                passwordEncoder.encode(request.password()), request.managerRole()
        );
        employeeRepository.save(employee);
        code.markUsed();
        return new RegistrationActionResponse("Manager account approved and created.", "APPROVED", null);
    }

    @Transactional(readOnly = true)
    public List<PendingRegistrationResponse> listPending(Jwt jwt) {
        Actor actor = Actor.from(jwt);
        requireApprovalManager(actor);
        return registrationRepository
                .findAllByOrganizationIdAndStatusOrderByCreatedAtAsc(actor.organizationId(), RegistrationStatus.PENDING_MANAGER_APPROVAL)
                .stream()
                .filter(r -> canApprove(actor, r))
                .map(PendingRegistrationResponse::from)
                .toList();
    }

    @Transactional
    public RegistrationActionResponse approve(Jwt jwt, Long registrationId) {
        Actor actor = Actor.from(jwt);
        requireApprovalManager(actor);
        RegistrationRequest registration = pending(registrationId);
        if (!canApprove(actor, registration)) throw forbidden("Registration request is outside your approval scope.");
        ensureIdentityAvailableForApproval(registration);

        Employee employee = Employee.createSelfRegistered(
                registration.getOrganizationId(), registration.getDealershipId(), registration.getEmployeeLoginId(),
                registration.getEmployeeName(), registration.getCompanyEmail(), registration.getPhone(),
                registration.getDepartment(), registration.getPasswordHash(), registration.getRequestedRole()
        );
        employeeRepository.save(employee);
        registration.approve(actor.employeeId());
        return new RegistrationActionResponse("Registration approved and employee account created.", "APPROVED", null);
    }

    @Transactional
    public RegistrationActionResponse reject(Jwt jwt, Long registrationId) {
        Actor actor = Actor.from(jwt);
        requireApprovalManager(actor);
        RegistrationRequest registration = pending(registrationId);
        if (!canApprove(actor, registration)) throw forbidden("Registration request is outside your approval scope.");
        registration.reject(actor.employeeId());
        return new RegistrationActionResponse("Registration request rejected.", "REJECTED", null);
    }

    @Transactional
    public PasswordResetRequestResponse requestPasswordReset(PasswordResetRequest request) {
        String generic = "If the Organization Code and Company Email match an active account, a reset link will be sent.";
        String devUrl = null;
        var organizationOpt = organizationRepository.findByOrganizationCode(request.organizationCode().trim());
        if (organizationOpt.isPresent()) {
            var employeeOpt = employeeRepository.findByOrganizationIdAndCompanyEmailIgnoreCaseAndDeletedAtIsNull(
                    organizationOpt.get().getId(), normalizeEmail(request.companyEmail()));
            if (employeeOpt.isPresent() && employeeOpt.get().isActive()) {
                byte[] bytes = new byte[32]; RANDOM.nextBytes(bytes);
                String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
                resetTokenRepository.save(PasswordResetToken.create(employeeOpt.get().getId(), TokenHash.sha256(rawToken), LocalDateTime.now().plusMinutes(30)));
                String resetUrl = frontendBaseUrl + "/password?mode=reset&token=" + rawToken;
                mailService.sendPasswordReset(employeeOpt.get().getCompanyEmail(), resetUrl);
                if (devExposeSecrets) devUrl = resetUrl;
            }
        }
        return new PasswordResetRequestResponse(generic, devUrl);
    }

    @Transactional
    public RegistrationActionResponse confirmPasswordReset(PasswordResetConfirmRequest request) {
        PasswordResetToken token = resetTokenRepository.findByTokenHash(TokenHash.sha256(request.token().trim()))
                .orElseThrow(() -> badRequest("Reset link is invalid or expired."));
        if (!token.isUsable()) throw badRequest("Reset link is invalid or expired.");
        Employee employee = employeeRepository.findByIdAndDeletedAtIsNull(token.getEmployeeId())
                .orElseThrow(() -> badRequest("Employee account is unavailable."));
        if (!employee.isActive()) throw forbidden("Inactive accounts cannot reset a password.");
        if (passwordEncoder.matches(request.newPassword(), employee.getPasswordHash())) {
            throw badRequest("New password must be different from the current password.");
        }
        employee.changePassword(passwordEncoder.encode(request.newPassword()));
        token.markUsed();
        return new RegistrationActionResponse("Password reset successfully.", "COMPLETED", null);
    }

    private Organization organization(String code) {
        return organizationRepository.findByOrganizationCode(code.trim()).orElseThrow(() -> badRequest("Organization Code is invalid."));
    }
    private String normalizeEmail(String email){ return email.trim().toLowerCase(Locale.ROOT); }
    private void validateCompanyEmail(Organization org, String email) {
        if (org.getEmailDomain() != null && !org.getEmailDomain().isBlank() && !email.endsWith("@" + org.getEmailDomain().toLowerCase(Locale.ROOT))) {
            throw badRequest("Company Email does not match the Organization email domain.");
        }
    }
    private Long resolveEmployeeDealership(Long organizationId, EmployeeDepartment department, Long dealershipId) {
        if (department == EmployeeDepartment.HEAD_OFFICE) return null;
        return requireDealership(organizationId, dealershipId);
    }
    private Long requireDealership(Long organizationId, Long dealershipId) {
        if (dealershipId == null) throw badRequest("Dealership Number is required for dealership roles.");
        Dealership dealership = dealershipRepository.findByIdAndOrganizationId(dealershipId, organizationId)
                .filter(Dealership::isActive).orElseThrow(() -> badRequest("Dealership Number is invalid or inactive."));
        return dealership.getId();
    }
    private void validateEmployeeRole(EmployeeDepartment department, EmployeeRole role) {
        boolean valid = switch (department) {
            case SERVICE -> List.of(EmployeeRole.SERVICE_ADVISOR, EmployeeRole.APPOINTMENT_COORDINATOR, EmployeeRole.TECHNICIAN).contains(role);
            case PARTS -> role == EmployeeRole.PARTS_SPECIALIST;
            case HEAD_OFFICE -> List.of(EmployeeRole.HEAD_OFFICE_STAFF, EmployeeRole.CALL_CENTER_AGENT).contains(role);
        };
        if (!valid) throw badRequest("Selected Position does not belong to the selected Department.");
    }
    private void ensureIdentityAvailable(Long orgId, String loginId, String email) {
        if (employeeRepository.existsByOrganizationIdAndEmployeeLoginIdIgnoreCase(orgId, loginId)) throw conflict("Employee ID already exists.");
        if (employeeRepository.existsByOrganizationIdAndCompanyEmailIgnoreCase(orgId, email)) throw conflict("Company Email already exists.");
        if (registrationRepository.existsByOrganizationIdAndEmployeeLoginIdIgnoreCaseAndStatusIn(orgId, loginId, ACTIVE_REQUEST_STATUSES)) throw conflict("Employee ID already has a pending registration.");
        if (registrationRepository.existsByOrganizationIdAndCompanyEmailIgnoreCaseAndStatusIn(orgId, email, ACTIVE_REQUEST_STATUSES)) throw conflict("Company Email already has a pending registration.");
    }
    private void ensureIdentityAvailableForApproval(RegistrationRequest r) {
        if (employeeRepository.existsByOrganizationIdAndEmployeeLoginIdIgnoreCase(r.getOrganizationId(), r.getEmployeeLoginId())) throw conflict("Employee ID was already registered before approval.");
        if (employeeRepository.existsByOrganizationIdAndCompanyEmailIgnoreCase(r.getOrganizationId(), r.getCompanyEmail())) throw conflict("Company Email was already registered before approval.");
    }
    private RegistrationRequest pending(Long id) {
        RegistrationRequest r = registrationRepository.findById(id).orElseThrow(() -> badRequest("Registration request not found."));
        if (r.getStatus() != RegistrationStatus.PENDING_MANAGER_APPROVAL) throw badRequest("Registration request is no longer pending approval.");
        return r;
    }
    private void requireApprovalManager(Actor actor) {
        if (!List.of(EmployeeRole.HEAD_OFFICE_ADMIN, EmployeeRole.HEAD_OFFICE_MANAGER, EmployeeRole.SERVICE_MANAGER).contains(actor.role())) {
            throw forbidden("Manager approval permission is required.");
        }
    }
    private boolean canApprove(Actor actor, RegistrationRequest r) {
        if (!actor.organizationId().equals(r.getOrganizationId())) return false;
        if (actor.role() == EmployeeRole.HEAD_OFFICE_ADMIN) return true;
        if (actor.role() == EmployeeRole.HEAD_OFFICE_MANAGER) return r.getDepartment() == EmployeeDepartment.HEAD_OFFICE;
        return actor.role() == EmployeeRole.SERVICE_MANAGER && actor.dealershipId() != null && actor.dealershipId().equals(r.getDealershipId());
    }
    private ApiException badRequest(String m){ return new ApiException(HttpStatus.BAD_REQUEST,"VALIDATION_ERROR",m); }
    private ApiException forbidden(String m){ return new ApiException(HttpStatus.FORBIDDEN,"FORBIDDEN",m); }
    private ApiException conflict(String m){ return new ApiException(HttpStatus.CONFLICT,"CONFLICT",m); }

    private record Actor(Long employeeId, Long organizationId, Long dealershipId, EmployeeRole role) {
        static Actor from(Jwt jwt) {
            if (jwt == null) throw new ApiException(HttpStatus.UNAUTHORIZED,"UNAUTHORIZED","Authentication is required.");
            Number employeeId = jwt.getClaim("employeeId"); Number organizationId = jwt.getClaim("organizationId");
            Number dealershipId = jwt.getClaim("dealershipId"); String role = jwt.getClaimAsString("role");
            if (employeeId == null || organizationId == null || role == null) throw new ApiException(HttpStatus.UNAUTHORIZED,"UNAUTHORIZED","JWT scope claims are incomplete.");
            return new Actor(employeeId.longValue(), organizationId.longValue(), dealershipId == null ? null : dealershipId.longValue(), EmployeeRole.valueOf(role));
        }
    }
}
