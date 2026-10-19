package com.poshancare.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "nutrition_records")
public class NutritionRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "child_id", nullable = false)
    private Child child;

    @Column(name = "record_date", nullable = false)
    private LocalDate recordDate;

    @Column(name = "food_consumed", length = 500)
    private String foodConsumed;

    @Column(name = "calories_consumed_kcal", precision = 8, scale = 2)
    private BigDecimal caloriesConsumedKcal;

    @Column(name = "calories_required_kcal", precision = 8, scale = 2)
    private BigDecimal caloriesRequiredKcal;

    @Column(name = "protein_consumed_g", precision = 7, scale = 2)
    private BigDecimal proteinConsumedG;

    @Column(name = "protein_required_g", precision = 7, scale = 2)
    private BigDecimal proteinRequiredG;

    @Column(name = "carbs_consumed_g", precision = 7, scale = 2)
    private BigDecimal carbsConsumedG;

    @Column(name = "carbs_required_g", precision = 7, scale = 2)
    private BigDecimal carbsRequiredG;

    @Column(length = 500)
    private String notes;

    @Column(name = "recorded_at", nullable = false)
    private LocalDateTime recordedAt;

    @PrePersist
    void onCreate() {
        if (recordedAt == null) recordedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Child getChild() { return child; }
    public void setChild(Child child) { this.child = child; }
    public LocalDate getRecordDate() { return recordDate; }
    public void setRecordDate(LocalDate recordDate) { this.recordDate = recordDate; }
    public String getFoodConsumed() { return foodConsumed; }
    public void setFoodConsumed(String foodConsumed) { this.foodConsumed = foodConsumed; }
    public BigDecimal getCaloriesConsumedKcal() { return caloriesConsumedKcal; }
    public void setCaloriesConsumedKcal(BigDecimal value) { this.caloriesConsumedKcal = value; }
    public BigDecimal getCaloriesRequiredKcal() { return caloriesRequiredKcal; }
    public void setCaloriesRequiredKcal(BigDecimal value) { this.caloriesRequiredKcal = value; }
    public BigDecimal getProteinConsumedG() { return proteinConsumedG; }
    public void setProteinConsumedG(BigDecimal value) { this.proteinConsumedG = value; }
    public BigDecimal getProteinRequiredG() { return proteinRequiredG; }
    public void setProteinRequiredG(BigDecimal value) { this.proteinRequiredG = value; }
    public BigDecimal getCarbsConsumedG() { return carbsConsumedG; }
    public void setCarbsConsumedG(BigDecimal value) { this.carbsConsumedG = value; }
    public BigDecimal getCarbsRequiredG() { return carbsRequiredG; }
    public void setCarbsRequiredG(BigDecimal value) { this.carbsRequiredG = value; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public LocalDateTime getRecordedAt() { return recordedAt; }
}
