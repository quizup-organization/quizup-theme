package io.github.quizup.topic.domain.exception;

import io.github.quizup.common.domain.exception.ProblemCategory;

import java.util.Map;

/**
 * Exceptions spécifiques au domaine Question
 */
public final class QuestionProblems {

    private QuestionProblems() {
        // Classe utilitaire
    }

    public static class QuestionNotFoundProblem extends QuestionProblem {
        public QuestionNotFoundProblem(String questionId) {
            super(questionId, "urn:quizup:question:notFound",
                    ProblemCategory.BUSINESS_RESOURCE_MISSING,
                    "Question not found",
                    "The question " + questionId + " was not found", null);
        }
    }

    public static class QuestionTextEmptyProblem extends QuestionProblem {
        public QuestionTextEmptyProblem(String questionId) {
            super(questionId, "urn:quizup:question:textEmpty",
                    "Question text cannot be empty",
                    "The question text must not be empty");
        }
    }

    public static class QuestionAnswersInvalidProblem extends QuestionProblem {
        public QuestionAnswersInvalidProblem(String questionId) {
            super(questionId, "urn:quizup:question:answersInvalid",
                    "Invalid answers",
                    "A question must have exactly 4 answers (A, B, C, D)");
        }
    }

    public static class QuestionCorrectAnswerMissingProblem extends QuestionProblem {
        public QuestionCorrectAnswerMissingProblem(String questionId) {
            super(questionId, "urn:quizup:question:correctAnswerMissing",
                    "Correct answer missing",
                    "The correct answer must be provided and match one of the choices");
        }
    }

    public static class QuestionAlreadyApprovedProblem extends QuestionProblem {
        public QuestionAlreadyApprovedProblem(String questionId) {
            super(questionId, "urn:quizup:question:alreadyApproved",
                    "Question already approved",
                    "The question " + questionId + " has already been approved");
        }
    }

    public static class QuestionAlreadyRejectedProblem extends QuestionProblem {
        public QuestionAlreadyRejectedProblem(String questionId) {
            super(questionId, "urn:quizup:question:alreadyRejected",
                    "Question already rejected",
                    "The question " + questionId + " has already been rejected");
        }
    }

    public static class NotEnoughApprovedQuestionsProblem extends QuestionProblem {
        public NotEnoughApprovedQuestionsProblem(String topicId, int current, int required) {
            super(topicId, "urn:quizup:question:notEnough",
                    "Not enough approved questions",
                    "Topic requires at least " + required + " approved questions to be published, but only " + current + " exist",
                    Map.of("current", current, "required", required));
        }
    }
}

