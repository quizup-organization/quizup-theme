package io.github.quizup.theme.infrastructure.in.api.response;

import io.github.quizup.theme.domain.model.TopicCategory;
import io.github.quizup.theme.domain.model.TopicStatus;

import java.io.Serializable;
import java.time.Instant;

/**
 * DTO de réponse pour un thème
 */
public record TopicResponse(
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
) implements Serializable {
}

