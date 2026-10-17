package io.github.quizup.theme.domain.model;

/** Filtre de la file des questions signalées. */
public record QuestionReportSummaryFilter(
        QuestionReportStatus status,
        String topicId,
        Boolean suspended
) {
}
