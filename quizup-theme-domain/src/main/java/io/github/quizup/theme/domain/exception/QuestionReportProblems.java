package io.github.quizup.theme.domain.exception;

import io.github.quizup.microservice.core.domain.exception.ProblemCategory;

import java.util.Map;

/** Problèmes métier des signalements de questions. */
public final class QuestionReportProblems {

    private QuestionReportProblems() {
    }

    public static class QuestionReportNotFoundProblem extends BaseReportProblem {
        public QuestionReportNotFoundProblem(String reportId) {
            super(reportId, "urn:quizup:question:reportNotFound",
                    ProblemCategory.BUSINESS_RESOURCE_MISSING,
                    "Question report not found",
                    "No question report with id " + reportId);
        }
    }

    public static class InvalidQuestionReportProblem extends BaseReportProblem {
        public InvalidQuestionReportProblem(String reportId, String detail) {
            super(reportId, "urn:quizup:question:reportInvalid",
                    ProblemCategory.VALIDATION,
                    "Invalid question report",
                    detail);
        }
    }

    public abstract static class BaseReportProblem extends io.github.quizup.microservice.core.domain.exception.BaseProblem {
        protected BaseReportProblem(String reportId,
                                    String type,
                                    ProblemCategory category,
                                    String title,
                                    String detail) {
            super(type, category, title, detail, Map.of("reportId", reportId));
        }
    }
}
