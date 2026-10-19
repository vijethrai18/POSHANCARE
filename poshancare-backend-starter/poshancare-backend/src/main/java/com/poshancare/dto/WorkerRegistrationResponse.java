package com.poshancare.dto;

/**
 * Public response returned after worker self-registration.
 * Conveys pending status clearly without exposing sensitive internal credentials.
 */
public class WorkerRegistrationResponse {

    private String message;
    private String workerId;
    private String fullName;
    private String mobile;
    private String centreName;
    private String status;

    public WorkerRegistrationResponse() {}

    public WorkerRegistrationResponse(String message, String workerId, String fullName, String mobile, String centreName, String status) {
        this.message = message;
        this.workerId = workerId;
        this.fullName = fullName;
        this.mobile = mobile;
        this.centreName = centreName;
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getWorkerId() {
        return workerId;
    }

    public void setWorkerId(String workerId) {
        this.workerId = workerId;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
