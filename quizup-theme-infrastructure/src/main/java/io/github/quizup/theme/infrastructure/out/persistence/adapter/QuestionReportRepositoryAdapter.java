package io.github.quizup.theme.infrastructure.out.persistence.adapter;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.quizup.theme.domain.model.QuestionReport;
import io.github.quizup.theme.domain.model.QuestionReportCount;
import io.github.quizup.theme.domain.model.QuestionReportSort;
import io.github.quizup.theme.domain.model.QuestionReportStatus;
import io.github.quizup.theme.domain.model.QuestionReportSummary;
import io.github.quizup.theme.domain.model.QuestionReportSummaryFilter;
import io.github.quizup.theme.domain.model.QuestionReportSummaryPage;
import io.github.quizup.theme.domain.port.out.QuestionReportRepositoryPort;
import io.github.quizup.theme.infrastructure.out.persistence.entity.QuestionReportEntity;
import io.github.quizup.theme.infrastructure.out.persistence.mapper.QuestionReportEntityMapper;
import io.github.quizup.theme.infrastructure.out.persistence.repository.QuestionReportJpaRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class QuestionReportRepositoryAdapter implements QuestionReportRepositoryPort {

    private final QuestionReportJpaRepository repository;
    private final EntityManager entityManager;
    private final ObjectMapper objectMapper;

    public QuestionReportRepositoryAdapter(QuestionReportJpaRepository repository,
                                           EntityManager entityManager,
                                           ObjectMapper objectMapper) {
        this.repository = repository;
        this.entityManager = entityManager;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public void save(QuestionReport report) {
        repository.findById(report.reportId()).ifPresentOrElse(
                existing -> {
                    QuestionReportEntityMapper.updateEntity(existing, report, objectMapper);
                    repository.saveAndFlush(existing);
                },
                () -> repository.saveAndFlush(QuestionReportEntityMapper.toEntity(report, objectMapper)));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<QuestionReport> findById(String reportId) {
        return repository.findById(reportId)
                .map(entity -> QuestionReportEntityMapper.toDomain(entity, objectMapper));
    }

    @Override
    @Transactional(readOnly = true)
    public List<QuestionReport> findByQuestionId(String questionId) {
        return repository.findByQuestionIdOrderByCreatedAtDesc(questionId).stream()
                .map(entity -> QuestionReportEntityMapper.toDomain(entity, objectMapper))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<QuestionReport> findOpenByQuestionId(String questionId) {
        return repository.findByQuestionIdAndStatusOrderByCreatedAtDesc(questionId, QuestionReportStatus.OPEN)
                .stream()
                .map(entity -> QuestionReportEntityMapper.toDomain(entity, objectMapper))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<QuestionReportCount> findCountsByQuestionIds(List<String> questionIds) {
        if (questionIds == null || questionIds.isEmpty()) {
            return List.of();
        }
        List<Object[]> rows = entityManager.createQuery("""
                        SELECT r.questionId,
                               SUM(CASE WHEN r.status = :open THEN 1 ELSE 0 END),
                               COUNT(r),
                               MAX(r.createdAt)
                        FROM QuestionReportEntity r
                        WHERE r.questionId IN :questionIds
                        GROUP BY r.questionId
                        """, Object[].class)
                .setParameter("open", QuestionReportStatus.OPEN)
                .setParameter("questionIds", questionIds)
                .getResultList();
        return rows.stream()
                .map(row -> new QuestionReportCount(
                        (String) row[0],
                        ((Number) row[1]).intValue(),
                        ((Number) row[2]).intValue(),
                        toInstant(row[3])))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public QuestionReportSummaryPage findSummaryPage(QuestionReportSummaryFilter filter,
                                                     QuestionReportSort sort,
                                                     int page,
                                                     int size) {
        QuestionReportSummaryFilter effective = filter == null
                ? new QuestionReportSummaryFilter(QuestionReportStatus.OPEN, null, null)
                : filter;
        Map<String, Object> parameters = new HashMap<>();
        List<String> conditions = new ArrayList<>();
        if (effective.topicId() != null && !effective.topicId().isBlank()) {
            conditions.add("r.topic_id = :topicId");
            parameters.put("topicId", effective.topicId());
        }
        if (effective.suspended() != null) {
            conditions.add("q.suspended = :suspended");
            parameters.put("suspended", effective.suspended());
        }
        String where = conditions.isEmpty() ? "" : " WHERE " + String.join(" AND ", conditions);
        String having = effective.status() == null
                ? ""
                : " HAVING SUM(CASE WHEN r.status = :status THEN 1 ELSE 0 END) > 0 ";
        if (effective.status() != null) {
            parameters.put("status", effective.status().name());
        }
        String from = " FROM question_report r JOIN question_entry q ON q.question_id = r.question_id ";
        String group = " GROUP BY r.question_id, r.topic_id ";
        String order = switch (sort == null ? QuestionReportSort.RECENT_REPORT : sort) {
            case OLDEST_REPORT -> " ORDER BY last_reported_at ASC, r.question_id ASC ";
            case MOST_REPORTED -> " ORDER BY open_count DESC, last_reported_at DESC, r.question_id ASC ";
            case RECENT_REPORT -> " ORDER BY last_reported_at DESC, r.question_id ASC ";
        };
        String select = """
                SELECT r.question_id, r.topic_id,
                       SUM(CASE WHEN r.status = 'OPEN' THEN 1 ELSE 0 END) AS open_count,
                       COUNT(*) AS total_count,
                       MAX(r.created_at) AS last_reported_at
                """;

        Query pageQuery = entityManager.createNativeQuery(select + from + where + group + having + order
                        + " LIMIT :pageSize OFFSET :offset")
                .setParameter("pageSize", size)
                .setParameter("offset", page * size);
        parameters.forEach(pageQuery::setParameter);
        @SuppressWarnings("unchecked")
        List<Object[]> rows = pageQuery.getResultList();
        List<QuestionReportSummary> content = rows.stream()
                .map(row -> new QuestionReportSummary(
                        (String) row[0],
                        (String) row[1],
                        ((Number) row[2]).intValue(),
                        ((Number) row[3]).intValue(),
                        toInstant(row[4])))
                .toList();

        String countSql = "SELECT COUNT(*) FROM (SELECT r.question_id "
                + from + where + group + having + ") grouped_reports";
        Query countQuery = entityManager.createNativeQuery(countSql);
        parameters.forEach(countQuery::setParameter);
        long total = ((Number) countQuery.getSingleResult()).longValue();
        return new QuestionReportSummaryPage(content, total);
    }

    @Override
    @Transactional(readOnly = true)
    public int countDistinctOpenReportersSince(String questionId, Instant since) {
        return repository.countDistinctOpenReportersSince(questionId, since);
    }

    @Override
    @Transactional(readOnly = true)
    public long countByStatus(QuestionReportStatus status) {
        return repository.countByStatus(status);
    }

    @Override
    @Transactional(readOnly = true)
    public long countDistinctQuestionIdByStatus(QuestionReportStatus status) {
        return repository.countDistinctQuestionIdByStatus(status);
    }

    private static Instant toInstant(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Instant instant) {
            return instant;
        }
        if (value instanceof java.sql.Timestamp timestamp) {
            return timestamp.toInstant();
        }
        if (value instanceof java.time.OffsetDateTime offsetDateTime) {
            return offsetDateTime.toInstant();
        }
        return Instant.parse(value.toString());
    }
}
