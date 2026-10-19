package com.poshancare.repository;

import com.poshancare.entity.Child;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChildRepository extends JpaRepository<Child, String> {

    // Search children by name (existing method preserved).
    List<Child> findByNameContainingIgnoreCase(String name);

    // Find only children registered by a specific worker.
    List<Child> findByCreatedByWorker_Id(String workerId);

    // Search by name only within a specific worker's children.
    List<Child> findByCreatedByWorker_IdAndNameContainingIgnoreCase(
            String workerId,
            String name
    );
}
