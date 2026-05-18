package io.github.quizup.theme.domain.event;

import io.github.quizup.theme.domain.model.TopicCategory;

import java.time.Instant;

public interface TopicEvent {
    String topicId();

    /**
     * Événement émis lors de la création d'un thème
     */
    record TopicCreatedEvent(
            String topicId,
            String name,
            String description,
            TopicCategory category,
            String creatorId,
            Instant createdAt
    ) implements TopicEvent {
    }

    /**
     * Événement émis lors de la publication d'un thème (DRAFT -> PUBLISHED)
     */
    record TopicPublishedEvent(
            String topicId,
            String updatedBy,
            Instant publishedAt
    ) implements TopicEvent {
    }
}

