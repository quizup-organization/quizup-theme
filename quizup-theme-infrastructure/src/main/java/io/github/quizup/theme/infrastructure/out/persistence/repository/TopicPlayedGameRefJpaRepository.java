package io.github.quizup.theme.infrastructure.out.persistence.repository;

import io.github.quizup.theme.infrastructure.out.persistence.entity.TopicPlayedGameRefEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TopicPlayedGameRefJpaRepository extends JpaRepository<TopicPlayedGameRefEntity, String> {

    @Query("SELECT COUNT(e) FROM TopicPlayedGameRefEntity e WHERE e.topicId = :topicId")
    int countByTopicId(@Param("topicId") String topicId);
}
