package com.poshancare.repository;

import com.poshancare.entity.NutritionRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface NutritionRecordRepository extends JpaRepository<NutritionRecord, Long> {
    List<NutritionRecord> findByChildChildIdOrderByRecordDateDesc(String childId);
}
