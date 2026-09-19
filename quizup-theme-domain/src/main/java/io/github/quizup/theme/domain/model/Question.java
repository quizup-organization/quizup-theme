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
        String imageUrl,
        Map<QuestionChoice, String> answers,
        QuestionChoice correctAnswer,
        QuestionStatus status,
        QuestionDifficulty difficulty,
        String creatorId,
        String updatedBy,
        Instant createdAt,
        Instant updatedAt
) {
}

