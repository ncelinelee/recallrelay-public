package com.recallrelay.registration;

import com.recallrelay.registration.dto.*;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class RegistrationController {
    private final RegistrationService service;
    public RegistrationController(RegistrationService service){ this.service = service; }

    @PostMapping("/auth/registrations/employee/start")
    public ResponseEntity<RegistrationActionResponse> startEmployee(@Valid @RequestBody EmployeeRegistrationStartRequest request){ return ResponseEntity.ok(service.startEmployee(request)); }

    @PostMapping("/auth/registrations/employee/verify")
    public ResponseEntity<RegistrationActionResponse> verifyEmployee(@Valid @RequestBody EmployeeRegistrationVerifyRequest request){ return ResponseEntity.ok(service.verifyEmployee(request)); }

    @PostMapping("/auth/registrations/manager")
    public ResponseEntity<RegistrationActionResponse> registerManager(@Valid @RequestBody ManagerRegistrationRequest request){ return ResponseEntity.ok(service.registerManager(request)); }

    @PostMapping("/auth/password-reset/request")
    public ResponseEntity<PasswordResetRequestResponse> requestReset(@Valid @RequestBody PasswordResetRequest request){ return ResponseEntity.ok(service.requestPasswordReset(request)); }

    @PostMapping("/auth/password-reset/confirm")
    public ResponseEntity<RegistrationActionResponse> confirmReset(@Valid @RequestBody PasswordResetConfirmRequest request){ return ResponseEntity.ok(service.confirmPasswordReset(request)); }

    @GetMapping("/registrations/pending")
    public ResponseEntity<List<PendingRegistrationResponse>> pending(@AuthenticationPrincipal Jwt jwt){ return ResponseEntity.ok(service.listPending(jwt)); }

    @PostMapping("/registrations/{id}/approve")
    public ResponseEntity<RegistrationActionResponse> approve(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id){ return ResponseEntity.ok(service.approve(jwt, id)); }

    @PostMapping("/registrations/{id}/reject")
    public ResponseEntity<RegistrationActionResponse> reject(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id){ return ResponseEntity.ok(service.reject(jwt, id)); }
}
