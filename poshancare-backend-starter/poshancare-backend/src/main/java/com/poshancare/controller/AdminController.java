package com.poshancare.controller;

import com.poshancare.dto.WorkerAdminResponse;
import com.poshancare.entity.AccountStatus;
import com.poshancare.service.AdminService;
import io.jsonwebtoken.Claims;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/worker-registrations")
    public List<WorkerAdminResponse> listRegistrations(
            @RequestParam(required = false, defaultValue = "PENDING") String status,
            Authentication authentication) {

        requireAdmin(authentication);

        AccountStatus accountStatus = null;
        if (status != null && !status.isBlank() && !"ALL".equalsIgnoreCase(status.trim())) {
            try {
                accountStatus = AccountStatus.valueOf(status.trim().toUpperCase());
            } catch (IllegalArgumentException ex) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Invalid status filter. Allowed values: PENDING, APPROVED, REJECTED, ALL"
                );
            }
        }

        return adminService.getWorkerRegistrations(accountStatus);
    }

    @PatchMapping("/worker-registrations/{workerId}/approve")
    public WorkerAdminResponse approveRegistration(
            @PathVariable String workerId,
            Authentication authentication) {

        requireAdmin(authentication);
        return adminService.approveWorker(workerId);
    }

    @PatchMapping("/worker-registrations/{workerId}/reject")
    public WorkerAdminResponse rejectRegistration(
            @PathVariable String workerId,
            Authentication authentication) {

        requireAdmin(authentication);
        return adminService.rejectWorker(workerId);
    }

    private void requireAdmin(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof Claims)) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Authentication required"
            );
        }

        Claims claims = (Claims) authentication.getPrincipal();
        String role = claims.get("role", String.class);

        if (!"ADMIN".equals(role)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Administrator access required"
            );
        }
    }
}
