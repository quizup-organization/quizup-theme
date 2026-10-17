package io.github.quizup.theme.domain.model;

/** Compteurs globaux de modération (badge console / cartes de synthèse). */
public record QuestionReportStats(
        long openReports,
        long reportedQuestions,
        long suspendedQuestions,
        long archivedQuestions
) {
}
