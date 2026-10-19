package com.poshancare.service;

import com.poshancare.dto.NutritionCreateRequest;
import com.poshancare.entity.*;
import com.poshancare.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.util.List;

@Service
public class NutritionRecordService {
    private final NutritionRecordRepository records;
    private final ChildRepository children;

    public NutritionRecordService(NutritionRecordRepository records, ChildRepository children) {
        this.records = records;
        this.children = children;
    }

    public NutritionRecord create(String childId, NutritionCreateRequest request) {
        Child child = children.findById(childId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Child not found"));
        NutritionRecord record = new NutritionRecord();
        record.setChild(child);
        record.setRecordDate(request.recordDate());
        record.setFoodConsumed(request.foodConsumed());
        record.setCaloriesConsumedKcal(request.caloriesConsumedKcal());
        record.setCaloriesRequiredKcal(request.caloriesRequiredKcal());
        record.setProteinConsumedG(request.proteinConsumedG());
        record.setProteinRequiredG(request.proteinRequiredG());
        record.setCarbsConsumedG(request.carbsConsumedG());
        record.setCarbsRequiredG(request.carbsRequiredG());
        record.setNotes(request.notes());
        return records.save(record);
    }

    public List<NutritionRecord> findForChild(String childId) {
        if (!children.existsById(childId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Child not found");
        }
        return records.findByChildChildIdOrderByRecordDateDesc(childId);
    }
}
