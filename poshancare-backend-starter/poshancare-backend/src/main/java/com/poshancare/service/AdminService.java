package com.poshancare.service;

import com.poshancare.dto.AdminLoginRequest;
import com.poshancare.dto.AdminLoginResponse;
import com.poshancare.dto.WorkerAdminResponse;
import com.poshancare.entity.AccountStatus;
import com.poshancare.entity.AppUser;
import com.poshancare.entity.UserRole;
import com.poshancare.repository.UserRepository;
import com.poshancare.security.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AdminService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AdminService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    /**
     * Authenticate administrator using mobile number and BCrypt password hash.
     * Returns JWT token with ADMIN role.
     */
    public AdminLoginResponse login(AdminLoginRequest request) {
        String mobile = request.getMobile() != null ? request.getMobile().trim() : "";
        String password = request.getPassword() != null ? request.getPassword() : "";

        // Generic error message to prevent account enumeration
        ResponseStatusException unauthorized = new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "Invalid administrator credentials"
        );

        AppUser admin = userRepository.findByMobile(mobile)
                .filter(user -> user.getRole() == UserRole.ADMIN)
                .orElseThrow(() -> unauthorized);

        if (admin.getPasswordHash() == null || admin.getPasswordHash().isBlank()) {
            throw unauthorized;
        }

        if (!passwordEncoder.matches(password, admin.getPasswordHash())) {
            throw unauthorized;
        }

        if (admin.getStatus() != AccountStatus.APPROVED) {
            throw unauthorized;
        }

        String token = jwtService.generateToken(
                admin.getMobile(),
                "ADMIN",
                admin.getId(),
                null
        );

        return new AdminLoginResponse(
                "Administrator authenticated successfully",
                token,
                "Bearer",
                "ADMIN",
                admin.getId(),
                admin.getFullName()
        );
    }

    /**
     * Retrieve worker registrations filtered by status (or all workers if status is null).
     * Exclusively targets WORKER accounts.
     */
    public List<WorkerAdminResponse> getWorkerRegistrations(AccountStatus status) {
        List<AppUser> workers;

        if (status != null) {
            workers = userRepository.findByRoleAndStatus(UserRole.WORKER, status);
        } else {
            workers = userRepository.findByRole(UserRole.WORKER);
        }

        return workers.stream()
                .sorted(Comparator.comparing(
                        AppUser::getCreatedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())
                ))
                .map(WorkerAdminResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Approve a worker registration.
     */
    public WorkerAdminResponse approveWorker(String workerId) {
        AppUser worker = userRepository.findById(workerId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Worker account not found with ID: " + workerId
                ));

        // Security check: Only worker accounts can be approved/modified
        if (worker.getRole() != UserRole.WORKER) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Cannot modify non-worker accounts"
            );
        }

        if (worker.getStatus() == AccountStatus.APPROVED) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Worker account is already approved"
            );
        }

        worker.setStatus(AccountStatus.APPROVED);
        AppUser saved = userRepository.save(worker);
        return WorkerAdminResponse.fromEntity(saved);
    }

    /**
     * Reject a worker registration.
     */
    public WorkerAdminResponse rejectWorker(String workerId) {
        AppUser worker = userRepository.findById(workerId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Worker account not found with ID: " + workerId
                ));

        // Security check: Only worker accounts can be modified
        if (worker.getRole() != UserRole.WORKER) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Cannot modify non-worker accounts"
            );
        }

        if (worker.getStatus() == AccountStatus.REJECTED) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Worker account is already rejected"
            );
        }

        worker.setStatus(AccountStatus.REJECTED);
        AppUser saved = userRepository.save(worker);
        return WorkerAdminResponse.fromEntity(saved);
    }
}
