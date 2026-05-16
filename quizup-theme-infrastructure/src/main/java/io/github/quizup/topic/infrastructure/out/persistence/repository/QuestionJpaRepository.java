package io.github.quizup.topic.infrastructure.out.persistence.repository;

import io.github.quizup.topic.infrastructure.out.persistence.entity.QuestionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository JPA pour les projections de questions
 */
@Repository
public interface QuestionJpaRepository extends JpaRepository<QuestionEntity, String>, JpaSpecificationExecutor<QuestionEntity> {

    @Query("SELECT COUNT(q) FROM QuestionEntity q WHERE q.topicId = :topicId AND q.status = io.github.quizup.common.domain.model.question.QuestionStatus.APPROVED")
    int countApprovedByTopicId(@Param("topicId") String topicId);

    @Query(value = "SELECT * FROM question_entry WHERE topic_id = :topicId AND status = 'APPROVED' ORDER BY RANDOM() LIMIT :count", nativeQuery = true)
    List<QuestionEntity> findRandomApprovedByTopicId(@Param("topicId") String topicId, @Param("count") int count);
}

