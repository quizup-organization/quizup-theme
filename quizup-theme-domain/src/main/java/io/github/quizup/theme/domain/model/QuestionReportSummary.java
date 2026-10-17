package io.github.quizup.theme.domain.model;

import java.time.Instant;

/** Ligne agrégée de la file des questions signalées (une par question). */
public record QuestionReportSummary(
        String questionId,
        String topicId,
        int openCount,
        int totalCount,
        Instant lastReportedAt
) {
}
