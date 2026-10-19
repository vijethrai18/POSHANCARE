package com.poshancare.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

public record GrowthCreateRequest(
    @NotNull @PastOrPresent LocalDate measurementDate,
    @NotNull @DecimalMin("30.0") @DecimalMax("200.0") BigDecimal heightCm,
    @NotNull @DecimalMin("1.0") @DecimalMax("150.0") BigDecimal weightKg,
    @Size(max = 500) String notes
) {}
