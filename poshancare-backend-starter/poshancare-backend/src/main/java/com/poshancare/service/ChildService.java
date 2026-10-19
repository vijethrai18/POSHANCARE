package com.poshancare.service;

import com.poshancare.dto.ChildCreateRequest;
import com.poshancare.entity.*;
import com.poshancare.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.UUID;

@Service
public class ChildService {

    private final ChildRepository children;
    private final UserRepository users;

    public ChildService(
            ChildRepository children,
            UserRepository users) {
        this.children = children;
        this.users = users;
    }


        // Find a child by ID.
    // The controller checks parent access before calling this method.
    public Child findById(String childId) {
        return children.findById(childId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Child not found"
                ));
    }


    @Transactional
    public Child create(
            ChildCreateRequest request,
            String workerId) {

        AppUser worker = users.findById(workerId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Authenticated worker does not exist"));

        if (worker.getRole() != UserRole.WORKER) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only workers can register children");
        }

        Child child = new Child();
        child.setChildId(
                "PC-" + UUID.randomUUID()
                        .toString().toUpperCase());
        child.setName(request.name().trim());
        child.setDateOfBirth(request.dateOfBirth());
        child.setGuardianName(request.guardianName().trim());
        child.setGuardianMobile(request.guardianMobile());
        child.setGender(request.gender().trim());
        child.setCentreName(request.centreName().trim());
        child.setCreatedByWorker(worker);

        return children.save(child);
    }

    // Only return children registered by this worker.
    public List<Child> findAll(String workerId) {
        return children.findByCreatedByWorker_Id(workerId);
    }

    // Only search this worker's own children.
    public List<Child> searchByName(
            String name,
            String workerId) {

        return children
                .findByCreatedByWorker_IdAndNameContainingIgnoreCase(
                        workerId,
                        name
                );
    }

    // Return a child only if this worker registered it.
    public Child findById(
            String childId,
            String workerId) {

        return children.findById(childId)
                .filter(child ->
                        child.getCreatedByWorker() != null
                        && workerId.equals(
                                child.getCreatedByWorker().getId()))
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Child not found or you do not have access"));
    }
}
