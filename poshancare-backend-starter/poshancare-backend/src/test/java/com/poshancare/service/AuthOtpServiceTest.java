package com.poshancare.service;

import com.poshancare.dto.OtpRequest;
import com.poshancare.dto.OtpVerifyRequest;
import com.poshancare.dto.WorkerRegisterRequest;
import com.poshancare.dto.WorkerRegistrationResponse;
import com.poshancare.entity.AccountStatus;
import com.poshancare.entity.AppUser;
import com.poshancare.entity.UserRole;
import com.poshancare.repository.ChildRepository;
import com.poshancare.repository.UserRepository;
import com.poshancare.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthOtpServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ChildRepository childRepository;

    @Mock
    private JwtService jwtService;

    private AuthOtpService authOtpService;

    @BeforeEach
    void setUp() {
        authOtpService = new AuthOtpService(userRepository, childRepository, jwtService);
    }

    @Test
    void testRegisterWorker_Success_AssignsWorkerRoleAndPendingStatus() {
        WorkerRegisterRequest request = new WorkerRegisterRequest(
                "Sunita Devi",
                "9876543210",
                "Anganwadi Centre 10",
                "kn"
        );

        when(userRepository.existsByMobile("9876543210")).thenReturn(false);
        when(userRepository.save(any(AppUser.class))).thenAnswer(invocation -> invocation.getArgument(0));

        WorkerRegistrationResponse response = authOtpService.registerWorker(request);

        assertNotNull(response);
        assertEquals("PENDING", response.getStatus());
        assertEquals("Sunita Devi", response.getFullName());
        assertEquals("9876543210", response.getMobile());
        assertEquals("Anganwadi Centre 10", response.getCentreName());
        assertTrue(response.getMessage().contains("awaiting administrator approval"));

        ArgumentCaptor<AppUser> captor = ArgumentCaptor.forClass(AppUser.class);
        verify(userRepository).save(captor.capture());
        AppUser savedUser = captor.getValue();

        assertEquals(UserRole.WORKER, savedUser.getRole(), "Role must strictly be WORKER");
        assertEquals(AccountStatus.PENDING, savedUser.getStatus(), "Status must strictly default to PENDING");
        assertNotNull(savedUser.getId());
        assertTrue(savedUser.getId().startsWith("WKR-"));
    }

    @Test
    void testRegisterWorker_DuplicateMobile_ThrowsConflict() {
        WorkerRegisterRequest request = new WorkerRegisterRequest(
                "Sunita Devi",
                "9876543210",
                "Anganwadi Centre 10",
                "en"
        );

        when(userRepository.existsByMobile("9876543210")).thenReturn(true);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                authOtpService.registerWorker(request)
        );

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        assertTrue(ex.getReason().contains("already registered"));
        verify(userRepository, never()).save(any(AppUser.class));
    }

    @Test
    void testRequestOtp_PendingWorker_ThrowsForbidden() {
        AppUser pendingWorker = new AppUser();
        pendingWorker.setId("worker-pending-1");
        pendingWorker.setMobile("9876543210");
        pendingWorker.setRole(UserRole.WORKER);
        pendingWorker.setStatus(AccountStatus.PENDING);

        when(userRepository.findByMobile("9876543210")).thenReturn(Optional.of(pendingWorker));

        OtpRequest request = new OtpRequest();
        request.setMobile("9876543210");
        request.setRole("WORKER");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                authOtpService.requestOtp(request)
        );

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        assertTrue(ex.getReason().contains("pending administrator approval"));
    }

    @Test
    void testRequestOtp_RejectedWorker_ThrowsForbidden() {
        AppUser rejectedWorker = new AppUser();
        rejectedWorker.setId("worker-rejected-1");
        rejectedWorker.setMobile("9876543210");
        rejectedWorker.setRole(UserRole.WORKER);
        rejectedWorker.setStatus(AccountStatus.REJECTED);

        when(userRepository.findByMobile("9876543210")).thenReturn(Optional.of(rejectedWorker));

        OtpRequest request = new OtpRequest();
        request.setMobile("9876543210");
        request.setRole("WORKER");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                authOtpService.requestOtp(request)
        );

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        assertTrue(ex.getReason().contains("rejected"));
    }

    @Test
    void testRequestOtp_ApprovedWorker_Succeeds() {
        AppUser approvedWorker = new AppUser();
        approvedWorker.setId("WORKER001");
        approvedWorker.setMobile("9000000001");
        approvedWorker.setRole(UserRole.WORKER);
        approvedWorker.setStatus(AccountStatus.APPROVED);

        when(userRepository.findByMobile("9000000001")).thenReturn(Optional.of(approvedWorker));

        OtpRequest request = new OtpRequest();
        request.setMobile("9000000001");
        request.setRole("WORKER");

        String result = authOtpService.requestOtp(request);
        assertNotNull(result);
        assertTrue(result.contains("OTP generated"));
    }

    @Test
    void testVerifyOtp_PendingWorker_ThrowsForbidden() {
        AppUser pendingWorker = new AppUser();
        pendingWorker.setId("worker-pending-1");
        pendingWorker.setMobile("9876543210");
        pendingWorker.setRole(UserRole.WORKER);
        pendingWorker.setStatus(AccountStatus.PENDING);

        when(userRepository.findByMobile("9876543210")).thenReturn(Optional.of(pendingWorker));

        OtpVerifyRequest request = new OtpVerifyRequest();
        request.setMobile("9876543210");
        request.setRole("WORKER");
        request.setOtp("123456");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                authOtpService.verifyOtp(request)
        );

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        assertTrue(ex.getReason().contains("not approved"));
    }
}
