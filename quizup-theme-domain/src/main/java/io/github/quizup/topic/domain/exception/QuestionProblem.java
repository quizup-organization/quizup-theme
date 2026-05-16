package io.github.quizup.topic.domain.exception;

import io.github.quizup.common.domain.exception.BaseProblem;
import io.github.quizup.common.domain.exception.ProblemCategory;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

/**
 * Classe de base pour toutes les exceptions métier liées au domaine Question
 */
@Getter
public abstract class QuestionProblem extends BaseProblem {

    private final String questionId;

    protected QuestionProblem(
            String questionId,
            String type,
            ProblemCategory category,
            String title,
            String detail,
            Map<String, Object> context) {
        super(type, category, title, detail, mergeContext(context, questionId));
        this.questionId = questionId;
    }

    protected QuestionProblem(
            String questionId,
            String type,
            String title,
            String detail,
            Map<String, Object> context) {
        this(questionId, type, ProblemCategory.BUSINESS_INVALID_COMMAND, title, detail, context);
    }

    protected QuestionProblem(
            String questionId,
            String type,
            String title,
            String detail) {
        this(questionId, type, ProblemCategory.BUSINESS_INVALID_COMMAND, title, detail, null);
    }

    protected QuestionProblem(
            String questionId,
            String type,
            String title) {
        this(questionId, type, title, null, null);
    }

    private static Map<String, Object> mergeContext(Map<String, Object> context, String questionId) {
        Map<String, Object> merged = new HashMap<>();
        if (context != null) {
            merged.putAll(context);
        }
        merged.put("questionId", questionId);
        return merged;
    }

}

