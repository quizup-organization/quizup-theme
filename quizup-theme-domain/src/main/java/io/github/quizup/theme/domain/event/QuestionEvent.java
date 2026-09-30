package io.github.quizup.theme.domain.event;


import io.github.quizup.microservice.core.domain.model.i18n.Language;
import io.github.quizup.theme.domain.model.QuestionChoice;
import io.github.quizup.theme.domain.model.QuestionContent;
import io.github.quizup.theme.domain.model.QuestionDifficulty;
import io.github.quizup.theme.domain.model.QuestionStatus;

import java.time.Instant;
import java.util.Map;

public interface QuestionEvent {
    String questionId();

    /**
     * Événement émis lors de l'ajout d'une question à un thème (un contenu par langue fournie).
     */
    record QuestionCreatedEvent(
            String questionId,
            String topicId,
            Map<Language, QuestionContent> contents,
            QuestionChoice correctAnswer,
            String imageUrl,
            String creatorId,
            Instant createdAt
    ) implements QuestionEvent {
    }

    /**
     * Événement émis lors de l'ajout (ou du remplacement) de contenus localisés.
     */
    record QuestionTranslationsAddedEvent(
            String questionId,
            Map<Language, QuestionContent> contents,
            String updatedBy,
            Instant updatedAt
    ) implements QuestionEvent {
    }

    /**
     * Événement émis lors de l'approbation d'une question
     */
    record QuestionApprovedEvent(
            String questionId,
            String topicId,
            QuestionStatus previousStatus,
            String updatedBy,
            Instant approvedAt
    ) implements QuestionEvent {
    }

    /**
     * Événement émis lors du rejet d'une question
     */
    record QuestionRejectedEvent(
            String questionId,
            String topicId,
            QuestionStatus previousStatus,
            String reason,
            String updatedBy,
            Instant rejectedAt
    ) implements QuestionEvent {
    }

    /**
     * Événement émis lorsque la difficulté déduite d'une question change.
     */
    record QuestionDifficultyUpdatedEvent(
            String questionId,
            QuestionDifficulty difficulty,
            Instant updatedAt
    ) implements QuestionEvent {
    }
}
