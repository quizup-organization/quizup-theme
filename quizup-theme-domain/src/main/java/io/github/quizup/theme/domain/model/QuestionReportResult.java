package io.github.quizup.theme.domain.model;

/** Résultat idempotent d'un signalement : identifiant du signalement et création effective. */
public record QuestionReportResult(
        String reportId,
        boolean created
) {
}
