package io.github.quizup.theme.domain.event;


import io.github.quizup.microservice.core.domain.model.i18n.Language;
import io.github.quizup.theme.domain.model.QuestionChoice;
import io.github.quizup.theme.domain.model.QuestionDifficulty;
import io.github.quizup.theme.domain.model.QuestionStatus;

import java.time.Instant;
import java.util.Map;

public interface QuestionEvent {
    String questionId();

    /**
     * Événement émis lors de l'ajout d'une question à un thème (contenu source).
     */
    record QuestionCreatedEvent(
            String questionId,
            String topicId,
            Language sourceLanguage,
            String text,
            Map<QuestionChoice, String> answers,
            QuestionChoice correctAnswer,
            String imageUrl,
            String creatorId,
            Instant createdAt
    ) implements QuestionEvent {
    }

    /**
     * Événement émis lors de l'ajout (ou du remplacement) d'une traduction de question.
     */
    record QuestionTranslationAddedEvent(
            String questionId,
            Language language,
            String text,
            Map<QuestionChoice, String> answers,
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
