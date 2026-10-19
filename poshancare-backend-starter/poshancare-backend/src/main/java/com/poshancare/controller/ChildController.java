package com.poshancare.controller;

import com.poshancare.dto.ChildCreateRequest;
import com.poshancare.entity.Child;
import com.poshancare.service.ChildService;

import io.jsonwebtoken.Claims;
import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/children")
public class ChildController {

    private final ChildService service;

    public ChildController(ChildService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Child create(
            @Valid @RequestBody ChildCreateRequest request,
            Authentication authentication) {

        Claims claims = getClaims(authentication);
        String workerId = requireWorker(claims);

        return service.create(request, workerId);
    }

    @GetMapping
    public List<Child> list(
            @RequestParam(required = false) String name,
            Authentication authentication) {

        Claims claims = getClaims(authentication);
        String workerId = requireWorker(claims);

        if (name == null || name.isBlank()) {
            return service.findAll(workerId);
        }

        return service.searchByName(name.trim(), workerId);
    }

    @GetMapping("/{childId}")
    public Child get(
            @PathVariable String childId,
            Authentication authentication) {

        Claims claims = getClaims(authentication);
        String role = claims.get("role", String.class);

        if ("WORKER".equals(role)) {
            String workerId = claims.get("userId", String.class);

            if (workerId == null || workerId.isBlank()) {
                throw new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Worker identity missing from token");
            }

            return service.findById(childId, workerId);
        }

        String linkedChildId =
                claims.get("childId", String.class);

        if ("PARENT".equals(role)
                && childId.equals(linkedChildId)) {
            return service.findById(childId);
        }

        throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "You are not allowed to access this child");
    }

    private String requireWorker(Claims claims) {
        String role = claims.get("role", String.class);

        if (!"WORKER".equals(role)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only workers can perform this action");
        }

        String workerId = claims.get("userId", String.class);

        if (workerId == null || workerId.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Worker identity missing from token");
        }

        return workerId;
    }

    private Claims getClaims(
            Authentication authentication) {

        if (authentication == null
                || !(authentication.getPrincipal() instanceof Claims)) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Authentication required");
        }

        return (Claims) authentication.getPrincipal();
    }
}
