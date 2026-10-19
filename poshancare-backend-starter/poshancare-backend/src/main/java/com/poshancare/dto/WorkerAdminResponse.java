package com.poshancare.dto;

import com.poshancare.entity.AppUser;
import java.time.LocalDateTime;

/**
 * Worker registration summary returned exclusively to administrators.
 * Safe view that never exposes passwords, hashes, or OTP secrets.
 */
public class WorkerAdminResponse {

    private String id;
    private String fullName;
    private String mobile;
    private String centreName;
    private String preferredLanguage;
    private String role;
    private String status;
    private LocalDateTime createdAt;

    public WorkerAdminResponse() {}

    public WorkerAdminResponse(String id, String fullName, String mobile, String centreName,
                               String preferredLanguage, String role, String status, LocalDateTime createdAt) {
        this.id = id;
        this.fullName = fullName;
        this.mobile = mobile;
        this.centreName = centreName;
        this.preferredLanguage = preferredLanguage;
        this.role = role;
        this.status = status;
        this.createdAt = createdAt;
    }

    public static WorkerAdminResponse fromEntity(AppUser user) {
        return new WorkerAdminResponse(
                user.getId(),
                user.getFullName(),
                user.getMobile(),
                user.getCentreName(),
                user.getPreferredLanguage(),
                user.getRole() != null ? user.getRole().name() : null,
                user.getStatus() != null ? user.getStatus().name() : null,
                user.getCreatedAt()
        );
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getMobile() { return mobile; }
    public void setMobile(String mobile) { this.mobile = mobile; }
    public String getCentreName() { return centreName; }
    public void setCentreName(String centreName) { this.centreName = centreName; }
    public String getPreferredLanguage() { return preferredLanguage; }
    public void setPreferredLanguage(String preferredLanguage) { this.preferredLanguage = preferredLanguage; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
