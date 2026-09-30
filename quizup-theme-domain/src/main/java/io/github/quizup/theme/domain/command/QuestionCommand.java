package io.github.quizup.theme.domain.command;

import io.github.quizup.microservice.core.domain.model.i18n.Language;
import io.github.quizup.theme.domain.model.QuestionChoice;
import io.github.quizup.theme.domain.model.QuestionContent;
import io.github.quizup.theme.domain.model.QuestionDifficulty;
import org.axonframework.modelling.command.TargetAggregateIdentifier;

import java.util.Map;

public interface QuestionCommand {
    String questionId();

    /**
     * Commande pour ajouter une question à un thème, avec un contenu par langue
     * (français, anglais, ou les deux).
     */
    record CreateQuestionCommand(
            @TargetAggregateIdentifier String questionId,
            String topicId,
            Map<Language, QuestionContent> contents,
            QuestionChoice correctAnswer,
            String imageUrl,
            String creatorId
    ) implements QuestionCommand {
    }

    /**
     * Commande d'ajout (ou remplacement) de contenus localisés d'une question existante.
     */
    record AddQuestionTranslationsCommand(
            @TargetAggregateIdentifier String questionId,
            Map<Language, QuestionContent> contents,
            String updatedBy
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

    /**
     * Commande de mise à jour de la difficulté déduite d'une question (calculée côté domaine à
     * partir des statistiques de réponses).
     */
    record UpdateQuestionDifficultyCommand(
            @TargetAggregateIdentifier String questionId,
            QuestionDifficulty difficulty
    ) implements QuestionCommand {
    }
}
