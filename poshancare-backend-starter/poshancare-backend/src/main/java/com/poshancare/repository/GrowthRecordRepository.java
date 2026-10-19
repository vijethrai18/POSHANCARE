package com.poshancare.repository;

import com.poshancare.entity.GrowthRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface GrowthRecordRepository extends JpaRepository<GrowthRecord, Long> {
    List<GrowthRecord> findByChildChildIdOrderByMeasurementDateDesc(String childId);
}
