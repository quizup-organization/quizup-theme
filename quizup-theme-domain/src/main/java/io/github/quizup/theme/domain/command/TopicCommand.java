package io.github.quizup.theme.domain.command;

import io.github.quizup.theme.domain.model.TopicCategory;
import org.axonframework.modelling.command.TargetAggregateIdentifier;

public interface TopicCommand {
    String topicId();

    /**
     * Commande pour créer un nouveau thème de quiz
     */
    record CreateTopicCommand(
            @TargetAggregateIdentifier String topicId,
            String name,
            String description,
            TopicCategory category,
            String creatorId
    ) implements TopicCommand {
    }

    /**
     * Commande pour publier un thème (transition DRAFT -> PUBLISHED)
     * Requiert au minimum 7 questions approuvées
     */
    record PublishTopicCommand(
            @TargetAggregateIdentifier String topicId,
            String requesterId
    ) implements TopicCommand {
    }
}

