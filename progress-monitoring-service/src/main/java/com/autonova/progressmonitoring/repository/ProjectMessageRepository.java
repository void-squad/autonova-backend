package com.autonova.progressmonitoring.repository;

import com.autonova.progressmonitoring.entity.ProjectMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ProjectMessageRepository extends JpaRepository<ProjectMessage, UUID> {
    List<ProjectMessage> findByProjectIdOrderByCreatedAtDesc(UUID projectId);
}
