
package com.poshancare.controller;

import com.poshancare.dto.GrowthCreateRequest;
import com.poshancare.entity.GrowthRecord;
import com.poshancare.service.GrowthRecordService;

import io.jsonwebtoken.Claims;
import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/children/{childId}/growth")
public class GrowthRecordController {

    private final GrowthRecordService service;

    public GrowthRecordController(GrowthRecordService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public GrowthRecord create(
            @PathVariable String childId,
            @Valid @RequestBody GrowthCreateRequest request,
            Authentication authentication) {

        Claims claims = getClaims(authentication);
        requireWorker(claims);

        return service.create(childId, request);
    }

    @GetMapping
    public List<GrowthRecord> list(
            @PathVariable String childId,
            Authentication authentication) {

        Claims claims = getClaims(authentication);
        String role = claims.get("role", String.class);

        if ("WORKER".equals(role)) {
            return service.findForChild(childId);
        }

        String linkedChildId =
                claims.get("childId", String.class);

        if ("PARENT".equals(role)
                && childId.equals(linkedChildId)) {
            return service.findForChild(childId);
        }

        throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "You are not allowed to access this child's growth records"
        );
    }

    private void requireWorker(Claims claims) {
        String role = claims.get("role", String.class);

        if (!"WORKER".equals(role)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only workers can create growth records"
            );
        }
    }

    private Claims getClaims(Authentication authentication) {
        if (authentication == null
                || !(authentication.getPrincipal() instanceof Claims)) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Authentication required"
            );
        }

        return (Claims) authentication.getPrincipal();
    }
}
