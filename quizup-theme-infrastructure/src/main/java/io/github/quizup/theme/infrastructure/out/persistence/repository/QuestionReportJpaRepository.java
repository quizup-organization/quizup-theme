package io.github.quizup.theme.infrastructure.out.persistence.repository;

import io.github.quizup.theme.domain.model.QuestionReportStatus;
import io.github.quizup.theme.infrastructure.out.persistence.entity.QuestionReportEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuestionReportJpaRepository extends JpaRepository<QuestionReportEntity, String> {

    List<QuestionReportEntity> findByQuestionIdOrderByCreatedAtDesc(String questionId);

    List<QuestionReportEntity> findByQuestionIdAndStatusOrderByCreatedAtDesc(
            String questionId, QuestionReportStatus status);

    long countByStatus(QuestionReportStatus status);

    @Query("SELECT COUNT(DISTINCT r.questionId) FROM QuestionReportEntity r WHERE r.status = :status")
    long countDistinctQuestionIdByStatus(@Param("status") QuestionReportStatus status);

    @Query("""
            SELECT COUNT(DISTINCT r.reporterId)
            FROM QuestionReportEntity r
            WHERE r.questionId = :questionId
              AND r.status = io.github.quizup.theme.domain.model.QuestionReportStatus.OPEN
              AND r.createdAt >= :since
            """)
    int countDistinctOpenReportersSince(@Param("questionId") String questionId,
                                        @Param("since") java.time.Instant since);
}
