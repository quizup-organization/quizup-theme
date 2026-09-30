package io.github.quizup.theme.domain.command;

import io.github.quizup.microservice.core.domain.model.i18n.Language;
import io.github.quizup.theme.domain.model.QuestionChoice;
import io.github.quizup.theme.domain.model.QuestionDifficulty;
import org.axonframework.modelling.command.TargetAggregateIdentifier;

import java.util.Map;

public interface QuestionCommand {
    String questionId();

    /**
     * Commande pour ajouter une question à un thème, avec sa langue source.
     */
    record CreateQuestionCommand(
            @TargetAggregateIdentifier String questionId,
            String topicId,
            Language sourceLanguage,
            String text,
            Map<QuestionChoice, String> answers,
            QuestionChoice correctAnswer,
            String imageUrl,
            String creatorId
    ) implements QuestionCommand {

        /** Compatibilité : source en français par défaut. */
        public CreateQuestionCommand(String questionId,
                                     String topicId,
                                     String text,
                                     Map<QuestionChoice, String> answers,
                                     QuestionChoice correctAnswer,
                                     String imageUrl,
                                     String creatorId) {
            this(questionId, topicId, Language.FR, text, answers, correctAnswer, imageUrl, creatorId);
        }
    }

    /**
     * Commande d'ajout (ou remplacement) d'une traduction de question. La langue source ne peut
     * pas être traduite : elle est portée par la création.
     */
    record AddQuestionTranslationCommand(
            @TargetAggregateIdentifier String questionId,
            Language language,
            String text,
            Map<QuestionChoice, String> answers,
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
