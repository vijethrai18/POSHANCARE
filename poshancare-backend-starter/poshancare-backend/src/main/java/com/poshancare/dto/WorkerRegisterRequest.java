package com.poshancare.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Request payload for Anganwadi worker self-registration.
 * Intentionally excludes role and status so clients cannot tamper with privileges.
 */
public class WorkerRegisterRequest {

    @NotBlank(message = "Full name is required")
    @Size(max = 120, message = "Full name must not exceed 120 characters")
    private String fullName;

    @NotBlank(message = "Mobile number is required")
    @Pattern(
        regexp = "^[0-9]{10}$",
        message = "Enter a valid 10-digit mobile number"
    )
    private String mobile;

    @NotBlank(message = "Anganwadi centre name is required")
    @Size(max = 120, message = "Centre name must not exceed 120 characters")
    private String centreName;

    @Size(max = 10, message = "Preferred language must not exceed 10 characters")
    private String preferredLanguage = "en";

    public WorkerRegisterRequest() {}

    public WorkerRegisterRequest(String fullName, String mobile, String centreName, String preferredLanguage) {
        this.fullName = fullName;
        this.mobile = mobile;
        this.centreName = centreName;
        this.preferredLanguage = preferredLanguage;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    public String getCentreName() {
        return centreName;
    }

    public void setCentreName(String centreName) {
        this.centreName = centreName;
    }

    public String getPreferredLanguage() {
        return preferredLanguage;
    }

    public void setPreferredLanguage(String preferredLanguage) {
        this.preferredLanguage = preferredLanguage;
    }
}
