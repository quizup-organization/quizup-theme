package io.github.quizup.theme.domain.model;

import lombok.Builder;

import java.time.Instant;

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
        int questionCount,
        Instant createdAt,
        Instant updatedAt
) {
}

