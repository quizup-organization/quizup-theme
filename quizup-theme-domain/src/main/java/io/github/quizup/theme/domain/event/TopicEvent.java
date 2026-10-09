package io.github.quizup.theme.domain.event;

import io.github.quizup.microservice.core.domain.model.i18n.Language;
import io.github.quizup.theme.domain.model.TopicCategory;

import java.time.Instant;
import java.util.Map;

public interface TopicEvent {
    String topicId();

    /**
     * Événement émis lors de la création d'un thème (un nom par langue).
     */
    record TopicCreatedEvent(
            String topicId,
            Map<Language, String> names,
            String description,
            TopicCategory category,
            String emoji,
            String color,
            String imageUrl,
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

    /**
     * Événement émis lors du changement du nom d'une langue du thème.
     */
    record TopicNameUpdatedEvent(
            String topicId,
            String updatedBy,
            Language language,
            String name,
            Instant updatedAt
    ) implements TopicEvent {
    }

    /**
     * Événement émis lors du changement de description du thème.
     */
    record TopicDescriptionUpdatedEvent(
            String topicId,
            String updatedBy,
            String description,
            Instant updatedAt
    ) implements TopicEvent {
    }

    /**
     * Événement émis lors du changement de catégorie du thème.
     */
    record TopicCategoryUpdatedEvent(
            String topicId,
            String updatedBy,
            TopicCategory category,
            Instant updatedAt
    ) implements TopicEvent {
    }

    /**
     * Événement émis lors du changement d'emoji du thème.
     */
    record TopicEmojiUpdatedEvent(
            String topicId,
            String updatedBy,
            String emoji,
            Instant updatedAt
    ) implements TopicEvent {
    }

    /**
     * Événement émis lors du changement de couleur du thème.
     */
    record TopicColorUpdatedEvent(
            String topicId,
            String updatedBy,
            String color,
            Instant updatedAt
    ) implements TopicEvent {
    }

    /**
     * Événement émis lors du changement d'illustration du thème.
     */
    record TopicImageUrlUpdatedEvent(
            String topicId,
            String updatedBy,
            String imageUrl,
            Instant updatedAt
    ) implements TopicEvent {
    }

}
