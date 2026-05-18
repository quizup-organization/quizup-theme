package io.github.quizup.theme.domain.event;


import io.github.quizup.theme.domain.model.QuestionChoice;

import java.time.Instant;
import java.util.Map;

public interface QuestionEvent {
    String questionId();

    /**
     * Événement émis lors de l'ajout d'une question à un thème
     */
    record QuestionCreatedEvent(
            String questionId,
            String topicId,
            String text,
            Map<QuestionChoice, String> answers,
            QuestionChoice correctAnswer,
            String creatorId,
            Instant createdAt
    ) implements QuestionEvent {
    }

    /**
     * Événement émis lors de l'approbation d'une question
     */
    record QuestionApprovedEvent(
            String questionId,
            String topicId,
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
            String reason,
            String updatedBy,
            Instant rejectedAt
    ) implements QuestionEvent {
    }
}

