package io.github.quizup.theme.domain.command;

import io.github.quizup.microservice.core.domain.model.i18n.Language;
import io.github.quizup.theme.domain.model.TopicCategory;
import org.axonframework.modelling.command.TargetAggregateIdentifier;

import java.util.Map;

public interface TopicCommand {
    String topicId();

    /**
     * Commande pour créer un nouveau thème de quiz. {@code names} porte un nom par langue
     * (FR obligatoire, EN optionnel).
     */
    record CreateTopicCommand(
            @TargetAggregateIdentifier String topicId,
            Map<Language, String> names,
            String description,
            TopicCategory category,
            String emoji,
            String color,
            String imageUrl,
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

    /**
     * Mise à jour du nom d'une langue (propriétaire du thème uniquement).
     */
    record UpdateTopicNameCommand(
            @TargetAggregateIdentifier String topicId,
            String requestedBy,
            Language language,
            String name
    ) implements TopicCommand {
    }

    /**
     * Mise à jour de la description (propriétaire du thème uniquement).
     */
    record UpdateTopicDescriptionCommand(
            @TargetAggregateIdentifier String topicId,
            String requestedBy,
            String description
    ) implements TopicCommand {
    }

    /**
     * Mise à jour de la catégorie (propriétaire du thème uniquement).
     */
    record UpdateTopicCategoryCommand(
            @TargetAggregateIdentifier String topicId,
            String requestedBy,
            TopicCategory category
    ) implements TopicCommand {
    }

    /**
     * Mise à jour de l'emoji (propriétaire du thème uniquement).
     */
    record UpdateTopicEmojiCommand(
            @TargetAggregateIdentifier String topicId,
            String requestedBy,
            String emoji
    ) implements TopicCommand {
    }

    /**
     * Mise à jour de la couleur d'accent (propriétaire du thème uniquement).
     */
    record UpdateTopicColorCommand(
            @TargetAggregateIdentifier String topicId,
            String requestedBy,
            String color
    ) implements TopicCommand {
    }

    /**
     * Mise à jour de l'illustration de couverture (propriétaire du thème uniquement).
     */
    record UpdateTopicImageUrlCommand(
            @TargetAggregateIdentifier String topicId,
            String requestedBy,
            String imageUrl
    ) implements TopicCommand {
    }

}
