
package com.poshancare.dto;

import jakarta.validation.constraints.*;
import java.time.LocalDate;

public record ChildCreateRequest(
    @NotBlank @Size(max = 100) String name,
    @NotNull @PastOrPresent LocalDate dateOfBirth,
    @NotBlank @Size(max = 100) String guardianName,
    @NotBlank @Pattern(regexp = "^[0-9]{10}$") String guardianMobile,
    @NotBlank @Size(max = 30) String gender,
    @NotBlank @Size(max = 120) String centreName
) {}
