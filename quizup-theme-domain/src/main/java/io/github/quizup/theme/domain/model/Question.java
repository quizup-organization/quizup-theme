package io.github.quizup.theme.domain.model;

import lombok.Builder;

import java.time.Instant;
import java.util.Map;

/**
 * Modèle domaine d'une question.
 */
@Builder(toBuilder = true)
public record Question(
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
) {
}

