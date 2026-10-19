package com.poshancare.service;

import com.poshancare.dto.GrowthCreateRequest;
import com.poshancare.entity.*;
import com.poshancare.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.util.List;

@Service
public class GrowthRecordService {
    private final GrowthRecordRepository records;
    private final ChildRepository children;

    public GrowthRecordService(GrowthRecordRepository records, ChildRepository children) {
        this.records = records;
        this.children = children;
    }

    public GrowthRecord create(String childId, GrowthCreateRequest request) {
        Child child = children.findById(childId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Child not found"));
        GrowthRecord record = new GrowthRecord();
        record.setChild(child);
        record.setMeasurementDate(request.measurementDate());
        record.setHeightCm(request.heightCm());
        record.setWeightKg(request.weightKg());
        record.setNotes(request.notes());
        return records.save(record);
    }

    public List<GrowthRecord> findForChild(String childId) {
        if (!children.existsById(childId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Child not found");
        }
        return records.findByChildChildIdOrderByMeasurementDateDesc(childId);
    }
}
