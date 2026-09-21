package io.github.quizup.theme.infrastructure.out.persistence.repository;

import io.github.quizup.theme.infrastructure.out.persistence.entity.TopicFollowerRefEntity;
import io.github.quizup.theme.infrastructure.out.persistence.entity.TopicFollowerRefId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TopicFollowerRefJpaRepository extends JpaRepository<TopicFollowerRefEntity, TopicFollowerRefId> {

    @Query("SELECT COUNT(e) FROM TopicFollowerRefEntity e WHERE e.id.topicId = :topicId")
    int countByTopicId(@Param("topicId") String topicId);
}
