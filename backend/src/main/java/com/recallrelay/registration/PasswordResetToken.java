package com.recallrelay.registration;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name="password_reset_tokens")
public class PasswordResetToken {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(name="employee_id", nullable=false) private Long employeeId;
    @Column(name="token_hash", nullable=false, unique=true) private String tokenHash;
    @Column(name="expires_at", nullable=false) private LocalDateTime expiresAt;
    @Column(name="used_at") private LocalDateTime usedAt;
    @Column(name="created_at", nullable=false) private LocalDateTime createdAt;
    protected PasswordResetToken() {}
    public static PasswordResetToken create(Long employeeId, String tokenHash, LocalDateTime expiresAt){
        PasswordResetToken t=new PasswordResetToken(); t.employeeId=employeeId; t.tokenHash=tokenHash; t.expiresAt=expiresAt; t.createdAt=LocalDateTime.now(); return t;
    }
    public boolean isUsable(){return usedAt==null && expiresAt.isAfter(LocalDateTime.now());}
    public void markUsed(){usedAt=LocalDateTime.now();}
    public Long getEmployeeId(){return employeeId;}
}
