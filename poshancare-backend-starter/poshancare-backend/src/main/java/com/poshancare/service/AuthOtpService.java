package com.poshancare.service;

import com.poshancare.dto.OtpRequest;
import com.poshancare.dto.OtpVerifyRequest;
import com.poshancare.dto.WorkerRegisterRequest;
import com.poshancare.dto.WorkerRegistrationResponse;
import com.poshancare.entity.AccountStatus;
import com.poshancare.entity.AppUser;
import com.poshancare.entity.Child;
import com.poshancare.entity.UserRole;
import com.poshancare.repository.ChildRepository;
import com.poshancare.repository.UserRepository;
import com.poshancare.security.JwtService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
public class AuthOtpService {

    private static final Logger log =
            LoggerFactory.getLogger(AuthOtpService.class);

    private static final long OTP_VALIDITY_MS =
            5 * 60 * 1000;

    private final UserRepository userRepository;
    private final ChildRepository childRepository;
    private final JwtService jwtService;

    private final SecureRandom secureRandom =
            new SecureRandom();

    private final ConcurrentMap<String, OtpEntry> otpStore =
            new ConcurrentHashMap<>();

    public AuthOtpService(
            UserRepository userRepository,
            ChildRepository childRepository,
            JwtService jwtService) {

        this.userRepository = userRepository;
        this.childRepository = childRepository;
        this.jwtService = jwtService;
    }

    // STEP 0: Worker self-registration (Server enforces WORKER role and PENDING status)
    public WorkerRegistrationResponse registerWorker(WorkerRegisterRequest request) {
        String mobile = request.getMobile() != null ? request.getMobile().trim() : "";

        // Reject duplicate mobile numbers
        if (userRepository.existsByMobile(mobile)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Mobile number is already registered"
            );
        }

        String workerId = "WKR-" + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();

        AppUser worker = new AppUser();
        worker.setId(workerId);
        worker.setFullName(request.getFullName().trim());
        worker.setMobile(mobile);
        worker.setCentreName(request.getCentreName().trim());
        worker.setPreferredLanguage(
                request.getPreferredLanguage() != null && !request.getPreferredLanguage().isBlank()
                        ? request.getPreferredLanguage().trim()
                        : "en"
        );
        // Server-enforced: Always WORKER and PENDING. Never trust client for role or status.
        worker.setRole(UserRole.WORKER);
        worker.setStatus(AccountStatus.PENDING);

        AppUser savedWorker = userRepository.save(worker);

        return new WorkerRegistrationResponse(
                "Registration submitted successfully. Your account is awaiting administrator approval.",
                savedWorker.getId(),
                savedWorker.getFullName(),
                savedWorker.getMobile(),
                savedWorker.getCentreName(),
                savedWorker.getStatus().name()
        );
    }

    // STEP 1: Request an OTP
    public String requestOtp(OtpRequest request) {

        String mobile = request.getMobile();
        String role = request.getRole();
        String childId = request.getChildId();

        if ("WORKER".equals(role)) {

            AppUser worker = userRepository.findByMobile(mobile)
                    .filter(user ->
                            user.getRole() == UserRole.WORKER)
                    .orElseThrow(() ->
                            new ResponseStatusException(
                                    HttpStatus.NOT_FOUND,
                                    "No worker account found for this mobile number"
                            ));

            // Enforce account approval check
            if (worker.getStatus() == AccountStatus.PENDING) {
                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Your registration is pending administrator approval."
                );
            }

            if (worker.getStatus() == AccountStatus.REJECTED) {
                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Your registration was rejected. Please contact an administrator."
                );
            }

            if (worker.getStatus() != AccountStatus.APPROVED) {
                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Worker account is not approved."
                );
            }

        } else if ("PARENT".equals(role)) {

            if (childId == null || childId.isBlank()) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Child ID is required for parent login"
                );
            }

            Child child = childRepository.findById(childId)
                    .orElseThrow(() ->
                            new ResponseStatusException(
                                    HttpStatus.NOT_FOUND,
                                    "Child ID not found"
                            ));

            if (!mobile.equals(child.getGuardianMobile())) {
                throw new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Mobile number does not match the registered guardian"
                );
            }

        } else {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Role must be WORKER or PARENT"
            );
        }

        // Generate a six-digit OTP.
        String otp = String.format(
                "%06d",
                secureRandom.nextInt(1_000_000)
        );

        String key = makeKey(role, mobile, childId);

        otpStore.put(
                key,
                new OtpEntry(
                        otp,
                        System.currentTimeMillis()
                                + OTP_VALIDITY_MS
                )
        );

        // DEVELOPMENT ONLY: OTP is logged, not sent by SMS.
        log.info(
                "Development OTP for {} login, mobile {}: {}",
                role,
                maskMobile(mobile),
                otp
        );

        return "OTP generated for development. Check the Spring Boot console.";
    }

    // STEP 2: Verify OTP and issue a JWT token.
    public Map<String, String> verifyOtp(
            OtpVerifyRequest request) {

        String role = request.getRole();
        String mobile = request.getMobile();
        String childId = request.getChildId();

        // Validate the account before accepting its OTP.
        if ("WORKER".equals(role)) {

            AppUser worker = userRepository.findByMobile(mobile)
                    .filter(user ->
                            user.getRole() == UserRole.WORKER)
                    .orElseThrow(() ->
                            new ResponseStatusException(
                                    HttpStatus.UNAUTHORIZED,
                                    "Invalid worker account"
                            ));

            // Strictly prevent pending/rejected workers from verifying OTP
            if (worker.getStatus() != AccountStatus.APPROVED) {
                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Worker account is not approved"
                );
            }

        } else if ("PARENT".equals(role)) {

            if (childId == null || childId.isBlank()) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Child ID is required"
                );
            }

            Child child = childRepository.findById(childId)
                    .orElseThrow(() ->
                            new ResponseStatusException(
                                    HttpStatus.UNAUTHORIZED,
                                    "Invalid Child ID or mobile number"
                            ));

            if (!mobile.equals(child.getGuardianMobile())) {
                throw new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Invalid Child ID or mobile number"
                );
            }

        } else {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid role"
            );
        }

        // Find the OTP for this role, mobile and child.
        String key = makeKey(role, mobile, childId);
        OtpEntry entry = otpStore.get(key);

        if (entry == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "OTP not found or already used. Request a new OTP."
            );
        }

        // Reject expired OTPs.
        if (System.currentTimeMillis() >= entry.expiresAt) {

            otpStore.remove(key, entry);

            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "OTP expired. Request a new OTP."
            );
        }

        // Reject incorrect OTPs.
        if (!entry.otp.equals(request.getOtp())) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Incorrect OTP"
            );
        }

        // Ensure this OTP can only be used once.
        if (!otpStore.remove(key, entry)) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "OTP already used. Request a new OTP."
            );
        }

        // WORKER LOGIN
        if ("WORKER".equals(role)) {

            AppUser worker = userRepository.findByMobile(mobile)
                    .filter(user ->
                            user.getRole() == UserRole.WORKER)
                    .orElseThrow(() ->
                            new ResponseStatusException(
                                    HttpStatus.UNAUTHORIZED,
                                    "Worker account not found"
                            ));

            if (worker.getStatus() != AccountStatus.APPROVED) {
                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Worker account is not approved"
                );
            }

            String token = jwtService.generateToken(
                    worker.getMobile(),
                    "WORKER",
                    worker.getId(),
                    null
            );

            return Map.of(
                    "message", "OTP verified successfully",
                    "role", "WORKER",
                    "userId", worker.getId(),
                    "token", token,
                    "tokenType", "Bearer"
            );
        }

        // PARENT LOGIN
        String token = jwtService.generateToken(
                mobile,
                "PARENT",
                null,
                childId
        );

        return Map.of(
                "message", "OTP verified successfully",
                "role", "PARENT",
                "childId", childId,
                "token", token,
                "tokenType", "Bearer"
        );
    }

    private String makeKey(
            String role,
            String mobile,
            String childId) {

        String safeChildId =
                childId == null ? "" : childId;

        return role + ":" + mobile + ":" + safeChildId;
    }

    private String maskMobile(String mobile) {
        return "******" + mobile.substring(6);
    }

    private static class OtpEntry {

        private final String otp;
        private final long expiresAt;

        private OtpEntry(String otp, long expiresAt) {
            this.otp = otp;
            this.expiresAt = expiresAt;
        }
    }
}
