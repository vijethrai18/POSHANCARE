package com.poshancare.entity;

/**
 * Represents the approval lifecycle status of an account.
 * - PENDING: Newly registered account awaiting administrator review.
 * - APPROVED: Administrator approved account; permitted to log in and access features.
 * - REJECTED: Registration declined by administrator; forbidden from logging in.
 */
public enum AccountStatus {
    PENDING,
    APPROVED,
    REJECTED
}
