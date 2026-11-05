package com.recallrelay.employee;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "employees")
public class Employee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    @Column(name = "dealership_id")
    private Long dealershipId;

    @Column(name = "employee_login_id", nullable = false)
    private String employeeLoginId;

    @Column(name = "employee_name", nullable = false)
    private String employeeName;

    @Column(name = "company_email", nullable = false)
    private String companyEmail;

    @Column(name = "phone")
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(name = "department")
    private EmployeeDepartment department;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private EmployeeRole role;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(name = "must_change_password", nullable = false)
    private boolean mustChangePassword;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    protected Employee() {
    }

    public static Employee create(
            Long organizationId,
            Long dealershipId,
            String employeeLoginId,
            String employeeName,
            String companyEmail,
            String passwordHash,
            EmployeeRole role
    ) {
        return createInternal(
                organizationId, dealershipId, employeeLoginId, employeeName, companyEmail,
                null, inferDepartment(role), passwordHash, role, true
        );
    }

    public static Employee createSelfRegistered(
            Long organizationId,
            Long dealershipId,
            String employeeLoginId,
            String employeeName,
            String companyEmail,
            String phone,
            EmployeeDepartment department,
            String passwordHash,
            EmployeeRole role
    ) {
        return createInternal(
                organizationId, dealershipId, employeeLoginId, employeeName, companyEmail,
                phone, department, passwordHash, role, false
        );
    }

    private static Employee createInternal(
            Long organizationId,
            Long dealershipId,
            String employeeLoginId,
            String employeeName,
            String companyEmail,
            String phone,
            EmployeeDepartment department,
            String passwordHash,
            EmployeeRole role,
            boolean mustChangePassword
    ) {
        Employee employee = new Employee();
        employee.organizationId = organizationId;
        employee.dealershipId = dealershipId;
        employee.employeeLoginId = employeeLoginId;
        employee.employeeName = employeeName;
        employee.companyEmail = companyEmail;
        employee.phone = phone;
        employee.department = department;
        employee.passwordHash = passwordHash;
        employee.role = role;
        employee.active = true;
        employee.mustChangePassword = mustChangePassword;
        return employee;
    }

    private static EmployeeDepartment inferDepartment(EmployeeRole role) {
        return switch (role) {
            case PARTS_SPECIALIST -> EmployeeDepartment.PARTS;
            case HEAD_OFFICE_ADMIN, HEAD_OFFICE_MANAGER, HEAD_OFFICE_STAFF, CALL_CENTER_AGENT -> EmployeeDepartment.HEAD_OFFICE;
            default -> EmployeeDepartment.SERVICE;
        };
    }

    public Long getId() { return id; }
    public Long getOrganizationId() { return organizationId; }
    public Long getDealershipId() { return dealershipId; }
    public String getEmployeeLoginId() { return employeeLoginId; }
    public String getEmployeeName() { return employeeName; }
    public String getCompanyEmail() { return companyEmail; }
    public String getPhone() { return phone; }
    public EmployeeDepartment getDepartment() { return department; }
    public String getPasswordHash() { return passwordHash; }
    public EmployeeRole getRole() { return role; }
    public boolean isActive() { return active; }
    public boolean isMustChangePassword() { return mustChangePassword; }
    public LocalDateTime getDeletedAt() { return deletedAt; }
    public boolean isDeleted() { return deletedAt != null; }

    public void updateProfile(String employeeName, String companyEmail, Long dealershipId, EmployeeRole role, boolean active) {
        this.employeeName = employeeName;
        this.companyEmail = companyEmail;
        this.dealershipId = dealershipId;
        this.role = role;
        this.department = inferDepartment(role);
        this.active = active;
    }

    public void changePassword(String encodedPassword) {
        this.passwordHash = encodedPassword;
        this.mustChangePassword = false;
    }

    public void softDelete(LocalDateTime deletedAt) {
        this.active = false;
        this.deletedAt = deletedAt;
    }
}
