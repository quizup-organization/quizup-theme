package io.github.quizup.topic.domain.command;

import io.github.quizup.topic.domain.model.QuestionChoice;
import org.axonframework.modelling.command.TargetAggregateIdentifier;

import java.util.Map;

public interface QuestionCommand {
    String questionId();

    /**
     * Commande pour ajouter une question à un thème
     */
    record CreateQuestionCommand(
            @TargetAggregateIdentifier String questionId,
            String topicId,
            String text,
            Map<QuestionChoice, String> answers,
            QuestionChoice correctAnswer,
            String creatorId
    ) implements QuestionCommand {
    }

    /**
     * Commande pour approuver une question (PENDING -> APPROVED)
     */
    record ApproveQuestionCommand(
            @TargetAggregateIdentifier String questionId,
            String requesterId
    ) implements QuestionCommand {
    }


    /**
     * Commande pour rejeter une question (PENDING -> REJECTED)
     */
    record RejectQuestionCommand(
            @TargetAggregateIdentifier String questionId,
            String requesterId,
            String reason
    ) implements QuestionCommand {
    }
}

