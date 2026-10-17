package io.github.quizup.theme.domain.model;

import java.time.Instant;

/** Compteurs de signalements d'une question. */
public record QuestionReportCount(
        String questionId,
        int openCount,
        int totalCount,
        Instant lastReportedAt
) {
}
