package com.poshancare.dto;

import com.poshancare.entity.FollowupStatus;
import jakarta.validation.constraints.*;
import java.time.LocalDate;

public record FollowupCreateRequest(
    @FutureOrPresent LocalDate dueDate,
    @NotBlank @Size(max = 500) String note,
    @NotBlank String assignedWorkerId,
    FollowupStatus status
) {}
