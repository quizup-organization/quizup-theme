package io.github.quizup.topic.domain.exception;

import io.github.quizup.common.domain.exception.BaseProblem;
import io.github.quizup.common.domain.exception.ProblemCategory;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

/**
 * Classe de base pour toutes les exceptions métier liées au domaine Topic
 */
@Getter
public abstract class TopicProblem extends BaseProblem {

    private final String topicId;

    protected TopicProblem(
            String topicId,
            String type,
            ProblemCategory category,
            String title,
            String detail,
            Map<String, Object> context) {
        super(
            type,
            category,
            title,
            detail,
            mergeContext(context, topicId)
        );
        this.topicId = topicId;
    }

    protected TopicProblem(
            String topicId,
            String type,
            String title,
            String detail,
            Map<String, Object> context) {
        this(topicId, type, ProblemCategory.BUSINESS_INVALID_COMMAND, title, detail, context);
    }

    protected TopicProblem(
            String topicId,
            String type,
            String title,
            String detail) {
        this(topicId, type, ProblemCategory.BUSINESS_INVALID_COMMAND, title, detail, null);
    }

    protected TopicProblem(
            String topicId,
            String type,
            String title) {
        this(topicId, type, title, null, null);
    }

    private static Map<String, Object> mergeContext(Map<String, Object> context, String topicId) {
        Map<String, Object> merged = new HashMap<>();
        if (context != null) {
            merged.putAll(context);
        }
        merged.put("topicId", topicId);
        return merged;
    }

}
