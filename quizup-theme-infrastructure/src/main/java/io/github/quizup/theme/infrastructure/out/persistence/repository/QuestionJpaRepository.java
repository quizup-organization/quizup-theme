package io.github.quizup.theme.infrastructure.out.persistence.repository;

import io.github.quizup.theme.domain.model.QuestionStatus;
import io.github.quizup.theme.infrastructure.out.persistence.entity.QuestionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

/**
 * Repository JPA pour les projections de questions.
 *
 * <p>La sélection par langue est stricte : une question n'est retenue que si elle possède un
 * contenu pour **toutes** les langues demandées (sous-requête groupée sur {@code question_content}).
 * </p>
 */
@Repository
public interface QuestionJpaRepository extends JpaRepository<QuestionEntity, String>, JpaSpecificationExecutor<QuestionEntity> {

    @Query("SELECT COUNT(q) FROM QuestionEntity q WHERE q.topicId = :topicId AND q.status = io.github.quizup.theme.domain.model.QuestionStatus.APPROVED")
    int countApprovedByTopicId(@Param("topicId") String topicId);

    @Query("SELECT COUNT(q) FROM QuestionEntity q WHERE q.topicId = :topicId AND q.status = :status")
    int countByTopicIdAndStatus(@Param("topicId") String topicId, @Param("status") QuestionStatus status);

    List<QuestionEntity> findByTopicId(String topicId);

    @Query(value = """
            SELECT q.* FROM question_entry q
            WHERE q.topic_id = :topicId
              AND q.status = 'APPROVED'
              AND q.question_id IN (
                  SELECT c.question_id FROM question_content c
                  WHERE c.language IN (:languages)
                  GROUP BY c.question_id
                  HAVING COUNT(DISTINCT c.language) = :languageCount
              )
            ORDER BY RANDOM()
            LIMIT :count
            """, nativeQuery = true)
    List<QuestionEntity> findRandomApprovedByTopicId(@Param("topicId") String topicId,
                                                     @Param("count") int count,
                                                     @Param("languages") Collection<String> languages,
                                                     @Param("languageCount") long languageCount);

    @Query(value = """
            SELECT COUNT(*) FROM question_entry q
            WHERE q.topic_id = :topicId
              AND q.status = 'APPROVED'
              AND q.question_id IN (
                  SELECT c.question_id FROM question_content c
                  WHERE c.language IN (:languages)
                  GROUP BY c.question_id
                  HAVING COUNT(DISTINCT c.language) = :languageCount
              )
            """, nativeQuery = true)
    int countApprovedByTopicAndLanguages(@Param("topicId") String topicId,
                                         @Param("languages") Collection<String> languages,
                                         @Param("languageCount") long languageCount);
}
