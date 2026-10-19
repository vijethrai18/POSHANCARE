package com.poshancare.service;

import com.poshancare.dto.AdminLoginRequest;
import com.poshancare.dto.AdminLoginResponse;
import com.poshancare.dto.WorkerAdminResponse;
import com.poshancare.entity.AccountStatus;
import com.poshancare.entity.AppUser;
import com.poshancare.entity.UserRole;
import com.poshancare.repository.UserRepository;
import com.poshancare.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    private AdminService adminService;

    @BeforeEach
    void setUp() {
        adminService = new AdminService(userRepository, passwordEncoder, jwtService);
    }

    @Test
    void testAdminLogin_Success() {
        AppUser admin = new AppUser();
        admin.setId("ADMIN001");
        admin.setFullName("System Administrator");
        admin.setMobile("9000000002");
        admin.setRole(UserRole.ADMIN);
        admin.setStatus(AccountStatus.APPROVED);
        admin.setPasswordHash("$2a$10$hashedPasswordSample");

        when(userRepository.findByMobile("9000000002")).thenReturn(Optional.of(admin));
        when(passwordEncoder.matches("TestOnlyPassword!42", "$2a$10$hashedPasswordSample")).thenReturn(true);
        when(jwtService.generateToken("9000000002", "ADMIN", "ADMIN001", null)).thenReturn("fake-test-jwt-token");

        AdminLoginRequest request = new AdminLoginRequest("9000000002", "TestOnlyPassword!42");
        AdminLoginResponse response = adminService.login(request);

        assertNotNull(response);
        assertEquals("fake-test-jwt-token", response.getToken());
        assertEquals("ADMIN", response.getRole());
        assertEquals("ADMIN001", response.getAdminId());
    }

    @Test
    void testAdminLogin_InvalidPassword_ThrowsUnauthorized() {
        AppUser admin = new AppUser();
        admin.setId("ADMIN001");
        admin.setMobile("9000000002");
        admin.setRole(UserRole.ADMIN);
        admin.setStatus(AccountStatus.APPROVED);
        admin.setPasswordHash("$2a$10$hashedPasswordSample");

        when(userRepository.findByMobile("9000000002")).thenReturn(Optional.of(admin));
        when(passwordEncoder.matches("WrongPassword", "$2a$10$hashedPasswordSample")).thenReturn(false);

        AdminLoginRequest request = new AdminLoginRequest("9000000002", "WrongPassword");
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                adminService.login(request)
        );

        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
        assertEquals("Invalid administrator credentials", ex.getReason());
    }

    @Test
    void testAdminLogin_WorkerAccountAttempt_ThrowsUnauthorized() {
        AppUser worker = new AppUser();
        worker.setId("WORKER001");
        worker.setMobile("9000000001");
        worker.setRole(UserRole.WORKER);
        worker.setStatus(AccountStatus.APPROVED);

        when(userRepository.findByMobile("9000000001")).thenReturn(Optional.of(worker));

        AdminLoginRequest request = new AdminLoginRequest("9000000001", "SomePassword");
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                adminService.login(request)
        );

        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
    }

    @Test
    void testGetWorkerRegistrations_FiltersOnlyWorkers() {
        AppUser worker1 = new AppUser();
        worker1.setId("WKR-1");
        worker1.setFullName("Worker One");
        worker1.setMobile("9876543210");
        worker1.setRole(UserRole.WORKER);
        worker1.setStatus(AccountStatus.PENDING);

        when(userRepository.findByRoleAndStatus(UserRole.WORKER, AccountStatus.PENDING))
                .thenReturn(List.of(worker1));

        List<WorkerAdminResponse> results = adminService.getWorkerRegistrations(AccountStatus.PENDING);

        assertEquals(1, results.size());
        assertEquals("WKR-1", results.get(0).getId());
        assertEquals("PENDING", results.get(0).getStatus());
        assertEquals("WORKER", results.get(0).getRole());
    }

    @Test
    void testApproveWorker_Success() {
        AppUser pendingWorker = new AppUser();
        pendingWorker.setId("WKR-1");
        pendingWorker.setFullName("Worker One");
        pendingWorker.setMobile("9876543210");
        pendingWorker.setRole(UserRole.WORKER);
        pendingWorker.setStatus(AccountStatus.PENDING);

        when(userRepository.findById("WKR-1")).thenReturn(Optional.of(pendingWorker));
        when(userRepository.save(any(AppUser.class))).thenAnswer(invocation -> invocation.getArgument(0));

        WorkerAdminResponse response = adminService.approveWorker("WKR-1");

        assertNotNull(response);
        assertEquals("APPROVED", response.getStatus());
        assertEquals(AccountStatus.APPROVED, pendingWorker.getStatus());
        verify(userRepository).save(pendingWorker);
    }

    @Test
    void testApproveWorker_AlreadyApproved_ThrowsConflict() {
        AppUser approvedWorker = new AppUser();
        approvedWorker.setId("WKR-1");
        approvedWorker.setRole(UserRole.WORKER);
        approvedWorker.setStatus(AccountStatus.APPROVED);

        when(userRepository.findById("WKR-1")).thenReturn(Optional.of(approvedWorker));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                adminService.approveWorker("WKR-1")
        );

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        assertTrue(ex.getReason().contains("already approved"));
    }

    @Test
    void testApproveWorker_TargetIsAdmin_ThrowsForbidden() {
        AppUser adminUser = new AppUser();
        adminUser.setId("ADMIN001");
        adminUser.setRole(UserRole.ADMIN);
        adminUser.setStatus(AccountStatus.APPROVED);

        when(userRepository.findById("ADMIN001")).thenReturn(Optional.of(adminUser));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                adminService.approveWorker("ADMIN001")
        );

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        assertTrue(ex.getReason().contains("non-worker"));
    }

    @Test
    void testRejectWorker_Success() {
        AppUser pendingWorker = new AppUser();
        pendingWorker.setId("WKR-1");
        pendingWorker.setFullName("Worker One");
        pendingWorker.setRole(UserRole.WORKER);
        pendingWorker.setStatus(AccountStatus.PENDING);

        when(userRepository.findById("WKR-1")).thenReturn(Optional.of(pendingWorker));
        when(userRepository.save(any(AppUser.class))).thenAnswer(invocation -> invocation.getArgument(0));

        WorkerAdminResponse response = adminService.rejectWorker("WKR-1");

        assertNotNull(response);
        assertEquals("REJECTED", response.getStatus());
        assertEquals(AccountStatus.REJECTED, pendingWorker.getStatus());
    }

    @Test
    void testRejectWorker_TargetIsAdmin_ThrowsForbidden() {
        AppUser adminUser = new AppUser();
        adminUser.setId("ADMIN001");
        adminUser.setRole(UserRole.ADMIN);
        adminUser.setStatus(AccountStatus.APPROVED);

        when(userRepository.findById("ADMIN001")).thenReturn(Optional.of(adminUser));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                adminService.rejectWorker("ADMIN001")
        );

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    void testApproveWorker_NotFound_ThrowsNotFound() {
        when(userRepository.findById("UNKNOWN")).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                adminService.approveWorker("UNKNOWN")
        );

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }
}
