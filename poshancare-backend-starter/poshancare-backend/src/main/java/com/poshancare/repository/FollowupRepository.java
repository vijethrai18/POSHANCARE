package com.poshancare.repository;

import com.poshancare.entity.Followup;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface FollowupRepository extends JpaRepository<Followup, Long> {
    List<Followup> findByChildChildIdOrderByDueDateAsc(String childId);
}
