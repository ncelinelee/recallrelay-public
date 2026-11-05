package com.recallrelay.registration;

import com.recallrelay.employee.EmployeeDepartment;
import com.recallrelay.employee.EmployeeRole;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "registration_requests")
public class RegistrationRequest {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name="organization_id", nullable=false) private Long organizationId;
    @Column(name="dealership_id") private Long dealershipId;
    @Column(name="employee_login_id", nullable=false) private String employeeLoginId;
    @Column(name="employee_name", nullable=false) private String employeeName;
    @Column(name="company_email", nullable=false) private String companyEmail;
    @Column(name="phone", nullable=false) private String phone;
    @Enumerated(EnumType.STRING) @Column(name="department", nullable=false) private EmployeeDepartment department;
    @Enumerated(EnumType.STRING) @Column(name="requested_role", nullable=false) private EmployeeRole requestedRole;
    @Column(name="password_hash", nullable=false) private String passwordHash;
    @Enumerated(EnumType.STRING) @Column(name="status", nullable=false) private RegistrationStatus status;
    @Column(name="verification_code_hash") private String verificationCodeHash;
    @Column(name="verification_expires_at") private LocalDateTime verificationExpiresAt;
    @Column(name="email_verified_at") private LocalDateTime emailVerifiedAt;
    @Column(name="approved_by_employee_id") private Long approvedByEmployeeId;
    @Column(name="approved_at") private LocalDateTime approvedAt;
    @Column(name="rejected_at") private LocalDateTime rejectedAt;
    @Column(name="created_at", nullable=false) private LocalDateTime createdAt;
    @Column(name="updated_at", nullable=false) private LocalDateTime updatedAt;

    protected RegistrationRequest() {}

    public static RegistrationRequest employee(Long organizationId, Long dealershipId, String loginId, String name,
                                               String email, String phone, EmployeeDepartment department,
                                               EmployeeRole role, String passwordHash, String codeHash,
                                               LocalDateTime codeExpiresAt) {
        RegistrationRequest r = new RegistrationRequest();
        r.organizationId = organizationId; r.dealershipId = dealershipId; r.employeeLoginId = loginId;
        r.employeeName = name; r.companyEmail = email; r.phone = phone; r.department = department;
        r.requestedRole = role; r.passwordHash = passwordHash; r.status = RegistrationStatus.PENDING_EMAIL_VERIFICATION;
        r.verificationCodeHash = codeHash; r.verificationExpiresAt = codeExpiresAt;
        r.createdAt = LocalDateTime.now(); r.updatedAt = r.createdAt;
        return r;
    }

    public void verifyEmail() {
        this.status = RegistrationStatus.PENDING_MANAGER_APPROVAL;
        this.emailVerifiedAt = LocalDateTime.now();
        this.verificationCodeHash = null;
        this.verificationExpiresAt = null;
        this.updatedAt = LocalDateTime.now();
    }

    public void approve(Long managerId) {
        this.status = RegistrationStatus.APPROVED; this.approvedByEmployeeId = managerId;
        this.approvedAt = LocalDateTime.now(); this.updatedAt = this.approvedAt;
    }
    public void reject(Long managerId) {
        this.status = RegistrationStatus.REJECTED; this.approvedByEmployeeId = managerId;
        this.rejectedAt = LocalDateTime.now(); this.updatedAt = this.rejectedAt;
    }

    public Long getId(){return id;} public Long getOrganizationId(){return organizationId;} public Long getDealershipId(){return dealershipId;}
    public String getEmployeeLoginId(){return employeeLoginId;} public String getEmployeeName(){return employeeName;}
    public String getCompanyEmail(){return companyEmail;} public String getPhone(){return phone;}
    public EmployeeDepartment getDepartment(){return department;} public EmployeeRole getRequestedRole(){return requestedRole;}
    public String getPasswordHash(){return passwordHash;} public RegistrationStatus getStatus(){return status;}
    public String getVerificationCodeHash(){return verificationCodeHash;} public LocalDateTime getVerificationExpiresAt(){return verificationExpiresAt;}
    public LocalDateTime getCreatedAt(){return createdAt;}
}
