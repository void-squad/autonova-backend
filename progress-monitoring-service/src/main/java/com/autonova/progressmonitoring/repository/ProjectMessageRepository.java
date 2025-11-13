package com.autonova.progressmonitoring.repository;

import com.autonova.progressmonitoring.entity.ProjectMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface ProjectMessageRepository extends JpaRepository<ProjectMessage, UUID> {
    List<ProjectMessage> findByProjectIdOrderByCreatedAtDesc(UUID projectId);

    // For timeline: fetch newest first (cursor/page)
    Slice<ProjectMessage> findByProjectIdOrderByCreatedAtDesc(UUID projectId, Pageable pageable);

    // For timeline: fetch older than a cursor
    Slice<ProjectMessage> findByProjectIdAndCreatedAtBeforeOrderByCreatedAtDesc(UUID projectId, OffsetDateTime before, Pageable pageable);

    // For timeline: fetch newer than a cursor (rare, but useful for catching up)
    Slice<ProjectMessage> findByProjectIdAndCreatedAtAfterOrderByCreatedAtAsc(UUID projectId, OffsetDateTime after, Pageable pageable);
}
