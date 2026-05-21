package io.github.quizup.theme.domain.model;

import lombok.Builder;

import java.time.Instant;
import java.util.Map;

/**
 * Modèle domaine d'un theme.
 */
@Builder(toBuilder = true)
public record Topic(
        String topicId,
        String name,
        String description,
        TopicCategory category,
        TopicStatus status,
        String creatorId,
        String updatedBy,
        Integer followersCounter,
        Map<QuestionStatus, Integer> questionsCounter,
        Instant createdAt,
        Instant updatedAt
) {
}

