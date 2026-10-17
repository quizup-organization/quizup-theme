package io.github.quizup.theme.domain.query;

import io.github.quizup.theme.domain.model.QuestionReportSort;
import io.github.quizup.theme.domain.model.QuestionReportSummaryFilter;

import java.util.List;

public interface QuestionReportQuery {

    record GetQuestionReportByIdQuery(String reportId) {
    }

    record GetQuestionReportsByQuestionQuery(String questionId) {
    }

    /** Compteurs batch pour une page de questions (catalogue admin / atelier). */
    record GetQuestionReportCountsQuery(List<String> questionIds) {
    }

    record GetReportedQuestionPageQuery(
            QuestionReportSummaryFilter filter,
            QuestionReportSort sort,
            int page,
            int size
    ) {
    }

    record GetQuestionReportStatsQuery() {
    }
}
