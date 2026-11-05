package com.recallrelay.employee;

import com.recallrelay.employee.dto.EmployeeCreateRequest;
import com.recallrelay.employee.dto.EmployeeManagementResponse;
import com.recallrelay.employee.dto.EmployeeUpdateRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/employees")
public class EmployeeController {

    private final EmployeeService employeeService;

    public EmployeeController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    @GetMapping
    public List<EmployeeManagementResponse> list(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) String q
    ) {
        return employeeService.list(jwt, q);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EmployeeManagementResponse create(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody EmployeeCreateRequest request
    ) {
        return employeeService.create(jwt, request);
    }

    @PatchMapping("/{employeeId}")
    public EmployeeManagementResponse update(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long employeeId,
            @Valid @RequestBody EmployeeUpdateRequest request
    ) {
        return employeeService.update(jwt, employeeId, request);
    }

}
