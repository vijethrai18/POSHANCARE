package com.poshancare.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

public record NutritionCreateRequest(
    @NotNull @PastOrPresent LocalDate recordDate,
    @Size(max = 500) String foodConsumed,
    @DecimalMin("0.0") BigDecimal caloriesConsumedKcal,
    @DecimalMin("0.0") BigDecimal caloriesRequiredKcal,
    @DecimalMin("0.0") BigDecimal proteinConsumedG,
    @DecimalMin("0.0") BigDecimal proteinRequiredG,
    @DecimalMin("0.0") BigDecimal carbsConsumedG,
    @DecimalMin("0.0") BigDecimal carbsRequiredG,
    @Size(max = 500) String notes
) {}
