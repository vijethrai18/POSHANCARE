package com.poshancare.service;

import com.poshancare.dto.FollowupCreateRequest;
import com.poshancare.entity.*;
import com.poshancare.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.util.List;

@Service
public class FollowupService {
    private final FollowupRepository followups;
    private final ChildRepository children;
    private final UserRepository users;

    public FollowupService(FollowupRepository followups, ChildRepository children, UserRepository users) {
        this.followups = followups;
        this.children = children;
        this.users = users;
    }

    public Followup create(String childId, FollowupCreateRequest request) {
        Child child = children.findById(childId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Child not found"));
        AppUser worker = users.findById(request.assignedWorkerId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Worker ID does not exist"));
        if (worker.getRole() != UserRole.WORKER) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Assigned user is not a worker");
        }

        Followup followup = new Followup();
        followup.setChild(child);
        followup.setAssignedWorker(worker);
        followup.setDueDate(request.dueDate());
        followup.setNote(request.note().trim());
        followup.setStatus(request.status() == null ? FollowupStatus.OPEN : request.status());
        return followups.save(followup);
    }

    public List<Followup> findForChild(String childId) {
        if (!children.existsById(childId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Child not found");
        }
        return followups.findByChildChildIdOrderByDueDateAsc(childId);
    }
}
