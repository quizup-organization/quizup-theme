package io.github.quizup.theme.infrastructure.out.persistence.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.quizup.microservice.core.domain.model.i18n.Language;
import io.github.quizup.theme.domain.model.QuestionReport;
import io.github.quizup.theme.domain.model.QuestionReportSnapshot;
import io.github.quizup.theme.domain.model.QuestionReportSnapshotSource;
import io.github.quizup.theme.infrastructure.out.persistence.entity.QuestionReportEntity;

import java.util.HashSet;

public final class QuestionReportEntityMapper {

    private QuestionReportEntityMapper() {
    }

    public static QuestionReport toDomain(QuestionReportEntity entity, ObjectMapper objectMapper) {
        return QuestionReport.builder()
                .reportId(entity.getReportId())
                .threadId(entity.getThreadId())
                .questionId(entity.getQuestionId())
                .topicId(entity.getTopicId())
                .reporterId(entity.getReporterId())
                .gameId(entity.getGameId())
                .reasons(new HashSet<>(entity.getReasons()))
                .comment(entity.getComment())
                .language(entity.getLanguage() == null ? null : Language.fromCode(entity.getLanguage()))
                .snapshot(readSnapshot(entity.getSnapshotJson(), objectMapper))
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .reviewedBy(entity.getReviewedBy())
                .reviewedAt(entity.getReviewedAt())
                .resolutionNote(entity.getResolutionNote())
                .build();
    }

    public static QuestionReportEntity toEntity(QuestionReport report, ObjectMapper objectMapper) {
        QuestionReportEntity entity = new QuestionReportEntity();
        entity.setReportId(report.reportId());
        entity.setThreadId(report.threadId());
        entity.setQuestionId(report.questionId());
        entity.setTopicId(report.topicId());
        entity.setReporterId(report.reporterId());
        entity.setGameId(report.gameId());
        entity.getReasons().clear();
        entity.getReasons().addAll(report.reasons());
        entity.setComment(report.comment());
        entity.setLanguage(report.language() == null ? null : report.language().code());
        entity.setSnapshotJson(writeSnapshot(report.snapshot(), objectMapper));
        entity.setSnapshotSource(report.snapshot() == null ? null : report.snapshot().source());
        entity.setStatus(report.status());
        entity.setCreatedAt(report.createdAt());
        entity.setReviewedBy(report.reviewedBy());
        entity.setReviewedAt(report.reviewedAt());
        entity.setResolutionNote(report.resolutionNote());
        return entity;
    }

    public static void updateEntity(QuestionReportEntity entity, QuestionReport report, ObjectMapper objectMapper) {
        entity.setComment(report.comment());
        entity.setLanguage(report.language() == null ? null : report.language().code());
        entity.setSnapshotJson(writeSnapshot(report.snapshot(), objectMapper));
        entity.setSnapshotSource(report.snapshot() == null ? null : report.snapshot().source());
        entity.setStatus(report.status());
        entity.setReviewedBy(report.reviewedBy());
        entity.setReviewedAt(report.reviewedAt());
        entity.setResolutionNote(report.resolutionNote());
    }

    private static QuestionReportSnapshot readSnapshot(String json, ObjectMapper objectMapper) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            QuestionReportSnapshot snapshot = objectMapper.readValue(json, QuestionReportSnapshot.class);
            return snapshot.source() == null
                    ? new QuestionReportSnapshot(
                            snapshot.questionText(), snapshot.imageUrl(), snapshot.answers(),
                            snapshot.translations(), snapshot.correctAnswer(), snapshot.language(),
                            QuestionReportSnapshotSource.CURRENT)
                    : snapshot;
        } catch (JsonProcessingException error) {
            throw new IllegalStateException("Unable to read question report snapshot", error);
        }
    }

    private static String writeSnapshot(QuestionReportSnapshot snapshot, ObjectMapper objectMapper) {
        if (snapshot == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(snapshot);
        } catch (JsonProcessingException error) {
            throw new IllegalStateException("Unable to write question report snapshot", error);
        }
    }
}
