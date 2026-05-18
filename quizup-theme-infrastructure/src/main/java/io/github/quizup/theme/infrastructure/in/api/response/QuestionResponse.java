package io.github.quizup.theme.infrastructure.in.api.response;

import io.github.quizup.theme.domain.model.QuestionChoice;
import io.github.quizup.theme.domain.model.QuestionStatus;

import java.io.Serializable;
import java.time.Instant;
import java.util.Map;

/**
 * DTO de réponse pour une question
 */
public record QuestionResponse(
        String questionId,
        String topicId,
        String text,
        Map<QuestionChoice, String> answers,
        QuestionChoice correctAnswer,
        QuestionStatus status,
        String creatorId,
        String updatedBy,
        Instant createdAt,
        Instant updatedAt
) implements Serializable {
}

