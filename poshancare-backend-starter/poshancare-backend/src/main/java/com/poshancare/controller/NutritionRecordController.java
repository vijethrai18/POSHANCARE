package com.poshancare.controller;

import com.poshancare.dto.NutritionCreateRequest;
import com.poshancare.entity.NutritionRecord;
import com.poshancare.service.NutritionRecordService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/children/{childId}/nutrition")
public class NutritionRecordController {
    private final NutritionRecordService service;
    public NutritionRecordController(NutritionRecordService service) { this.service = service; }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public NutritionRecord create(@PathVariable String childId,
                                  @Valid @RequestBody NutritionCreateRequest request) {
        return service.create(childId, request);
    }

    @GetMapping
    public List<NutritionRecord> list(@PathVariable String childId) {
        return service.findForChild(childId);
    }
}
