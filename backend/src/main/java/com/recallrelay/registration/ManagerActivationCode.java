package com.recallrelay.registration;

import com.recallrelay.employee.EmployeeRole;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name="manager_activation_codes")
public class ManagerActivationCode {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(name="organization_id", nullable=false) private Long organizationId;
    @Column(name="dealership_id") private Long dealershipId;
    @Column(name="employee_login_id", nullable=false) private String employeeLoginId;
    @Enumerated(EnumType.STRING) @Column(name="manager_role", nullable=false) private EmployeeRole managerRole;
    @Column(name="code_hash", nullable=false) private String codeHash;
    @Column(name="expires_at", nullable=false) private LocalDateTime expiresAt;
    @Column(name="used_at") private LocalDateTime usedAt;
    @Column(name="active", nullable=false) private boolean active;
    protected ManagerActivationCode() {}
    public boolean isUsable(){ return active && usedAt == null && expiresAt.isAfter(LocalDateTime.now()); }
    public void markUsed(){ this.usedAt = LocalDateTime.now(); this.active = false; }
    public Long getOrganizationId(){return organizationId;} public Long getDealershipId(){return dealershipId;}
    public String getEmployeeLoginId(){return employeeLoginId;} public EmployeeRole getManagerRole(){return managerRole;}
    public String getCodeHash(){return codeHash;}
}
