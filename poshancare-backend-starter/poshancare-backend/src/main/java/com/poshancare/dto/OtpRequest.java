
package com.poshancare.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class OtpRequest {

    @NotBlank(message = "Mobile number is required")
    @Pattern(
        regexp = "^[0-9]{10}$",
        message = "Enter a valid 10-digit mobile number"
    )
    private String mobile;

    @NotBlank(message = "Role is required")
    @Pattern(
        regexp = "^(WORKER|PARENT)$",
        message = "Role must be WORKER or PARENT"
    )
    private String role;

    // Required for PARENT login; not required for WORKER login.
    private String childId;

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getChildId() {
        return childId;
    }

    public void setChildId(String childId) {
        this.childId = childId;
    }
}
