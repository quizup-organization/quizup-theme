package io.github.quizup.theme.domain.model;

import java.util.List;

/** Page de lignes agrégées de signalements. */
public record QuestionReportSummaryPage(
        List<QuestionReportSummary> content,
        long totalElements
) {
    public QuestionReportSummaryPage {
        content = content == null ? List.of() : List.copyOf(content);
    }
}
