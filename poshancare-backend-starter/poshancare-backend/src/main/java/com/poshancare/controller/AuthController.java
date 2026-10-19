package com.poshancare.controller;

import com.poshancare.dto.AdminLoginRequest;
import com.poshancare.dto.AdminLoginResponse;
import com.poshancare.dto.OtpRequest;
import com.poshancare.dto.OtpVerifyRequest;
import com.poshancare.dto.WorkerRegisterRequest;
import com.poshancare.dto.WorkerRegistrationResponse;
import com.poshancare.service.AdminService;
import com.poshancare.service.AuthOtpService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthOtpService authOtpService;
    private final AdminService adminService;

    public AuthController(AuthOtpService authOtpService, AdminService adminService) {
        this.authOtpService = authOtpService;
        this.adminService = adminService;
    }

    @PostMapping("/worker-register")
    public ResponseEntity<WorkerRegistrationResponse> registerWorker(
            @Valid @RequestBody WorkerRegisterRequest request) {

        WorkerRegistrationResponse response =
                authOtpService.registerWorker(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/admin-login")
    public ResponseEntity<AdminLoginResponse> adminLogin(
            @Valid @RequestBody AdminLoginRequest request) {

        AdminLoginResponse response = adminService.login(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/request-otp")
    public ResponseEntity<Map<String, String>> requestOtp(
            @Valid @RequestBody OtpRequest request) {

        String message = authOtpService.requestOtp(request);

        return ResponseEntity.ok(Map.of(
                "message", message
        ));
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<Map<String, String>> verifyOtp(
            @Valid @RequestBody OtpVerifyRequest request) {

        Map<String, String> result =
                authOtpService.verifyOtp(request);

        return ResponseEntity.ok(result);
    }
}
