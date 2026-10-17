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

    @Query("SELECT COUNT(q) FROM QuestionEntity q WHERE q.topicId = :topicId AND q.status = io.github.quizup.theme.domain.model.QuestionStatus.APPROVED AND q.suspended = false AND q.archived = false")
    int countApprovedByTopicId(@Param("topicId") String topicId);

    @Query("SELECT COUNT(q) FROM QuestionEntity q WHERE q.topicId = :topicId AND q.status = :status")
    int countByTopicIdAndStatus(@Param("topicId") String topicId, @Param("status") QuestionStatus status);

    List<QuestionEntity> findByTopicId(String topicId);

    @Query("SELECT COUNT(q) FROM QuestionEntity q WHERE q.suspended = true AND q.archived = false")
    int countSuspended();

    @Query("SELECT COUNT(q) FROM QuestionEntity q WHERE q.archived = true")
    int countArchived();

    /**
     * Tirage best-effort : langues strictes, puis préférences de tri — jamais jouées d'abord,
     * puis difficulté cible, puis difficulté inconnue, puis les autres ; aléatoire à chaque rang.
     * Les préférences arrivent en CSV (vide = neutre) pour éviter le binding de tableaux.
     */
    @Query(value = """
            SELECT q.* FROM question_entry q
            WHERE q.topic_id = :topicId
              AND q.status = 'APPROVED'
              AND q.suspended = FALSE
              AND q.archived = FALSE
              AND q.question_id IN (
                  SELECT c.question_id FROM question_content c
                  WHERE c.language IN (:languages)
                  GROUP BY c.question_id
                  HAVING COUNT(DISTINCT c.language) = :languageCount
              )
            ORDER BY (
                CASE WHEN q.question_id = ANY(string_to_array(NULLIF(:excludedCsv, ''), ',')) THEN 1 ELSE 0 END
            ) ASC,
            (
                CASE WHEN q.difficulty = ANY(string_to_array(NULLIF(:difficultiesCsv, ''), ',')) THEN 0 ELSE 1 END
            ) ASC,
            (
                CASE WHEN q.difficulty IS NULL THEN 0 ELSE 1 END
            ) ASC,
            RANDOM()
            LIMIT :count
            """, nativeQuery = true)
    List<QuestionEntity> findRandomApprovedByTopicId(@Param("topicId") String topicId,
                                                     @Param("count") int count,
                                                     @Param("languages") Collection<String> languages,
                                                     @Param("languageCount") long languageCount,
                                                     @Param("excludedCsv") String excludedCsv,
                                                     @Param("difficultiesCsv") String difficultiesCsv);

    @Query(value = """
            SELECT COUNT(*) FROM question_entry q
            WHERE q.topic_id = :topicId
              AND q.status = 'APPROVED'
              AND q.suspended = FALSE
              AND q.archived = FALSE
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

    @Query("""
            select q.imageUrl, count(q)
            from QuestionEntity q
            where q.imageUrl in :urls
            group by q.imageUrl
            """)
    List<Object[]> countByImageUrlIn(@Param("urls") Collection<String> urls);
}
