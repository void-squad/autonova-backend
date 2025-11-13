package com.voidsquad.chatbot.repository;

import com.voidsquad.chatbot.entities.StaticInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface StaticInfoRepository extends JpaRepository<StaticInfo, UUID> {

    @Query(value = "INSERT INTO static_info_vector_db (id, topic, description, embedding) VALUES (:#{#info.id}, :#{#info.topic}, :#{#info.description}, CAST(:#{#info.embedding} AS vector)) RETURNING *",
            nativeQuery = true)
    StaticInfo saveWithVector(@Param("info") StaticInfo info);

    @Query(value = "SELECT * FROM static_info_vector_db ORDER BY embedding <=> CAST(:embedding AS vector) LIMIT :limit",
            nativeQuery = true)
    List<StaticInfo> findSimilarStaticInfo(@Param("embedding") float[] embedding,
                                           @Param("limit") int limit);
}
