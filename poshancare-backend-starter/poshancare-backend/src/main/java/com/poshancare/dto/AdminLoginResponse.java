package com.poshancare.dto;

public class AdminLoginResponse {

    private String message;
    private String token;
    private String tokenType;
    private String role;
    private String adminId;
    private String fullName;

    public AdminLoginResponse() {}

    public AdminLoginResponse(String message, String token, String tokenType, String role, String adminId, String fullName) {
        this.message = message;
        this.token = token;
        this.tokenType = tokenType;
        this.role = role;
        this.adminId = adminId;
        this.fullName = fullName;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getTokenType() {
        return tokenType;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getAdminId() {
        return adminId;
    }

    public void setAdminId(String adminId) {
        this.adminId = adminId;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }
}
