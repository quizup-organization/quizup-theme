package io.github.quizup.theme.domain.port.in;

import io.github.quizup.microservice.core.domain.model.i18n.Language;
import io.github.quizup.theme.domain.command.TopicCommand;
import io.github.quizup.theme.domain.model.TopicCategory;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

public interface CreateTopicUseCase {

    CompletableFuture<String> create(TopicCommand.CreateTopicCommand command);

    default CompletableFuture<String> create(
            String topicId,
            Map<Language, String> names,
            String description,
            TopicCategory category,
            String creatorId
    ) {
        return create(topicId, names, description, category, null, null, creatorId);
    }

    default CompletableFuture<String> create(
            String topicId,
            Map<Language, String> names,
            String description,
            TopicCategory category,
            String emoji,
            String color,
            String creatorId
    ) {
        return create(topicId, names, description, category, emoji, color, null, creatorId);
    }

    default CompletableFuture<String> create(
            String topicId,
            Map<Language, String> names,
            String description,
            TopicCategory category,
            String emoji,
            String color,
            String imageUrl,
            String creatorId
    ) {
        return create(
                new TopicCommand.CreateTopicCommand(
                        topicId,
                        names,
                        description,
                        category,
                        emoji,
                        color,
                        imageUrl,
                        creatorId
                )
        );
    }

    default void createAndWait(
            String topicId,
            Map<Language, String> names,
            String description,
            TopicCategory category,
            String creatorId
    ) {
        create(topicId, names, description, category, creatorId).join();
    }

    default void createAndWait(
            String topicId,
            Map<Language, String> names,
            String description,
            TopicCategory category,
            String emoji,
            String color,
            String imageUrl,
            String creatorId
    ) {
        create(topicId, names, description, category, emoji, color, imageUrl, creatorId).join();
    }
}
