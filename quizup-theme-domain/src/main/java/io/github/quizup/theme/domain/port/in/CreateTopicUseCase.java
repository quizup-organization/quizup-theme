package io.github.quizup.theme.domain.port.in;

import io.github.quizup.theme.domain.command.TopicCommand;
import io.github.quizup.theme.domain.model.TopicCategory;

import java.util.concurrent.CompletableFuture;

public interface CreateTopicUseCase {

    CompletableFuture<String> create(TopicCommand.CreateTopicCommand command);

    default CompletableFuture<String> create(
            String topicId,
            String name,
            String description,
            TopicCategory category,
            String creatorId
    ) {
        return create(
                new TopicCommand.CreateTopicCommand(
                        topicId,
                        name,
                        description,
                        category,
                        creatorId
                )
        );
    }

    default void createAndWait(
            String topicId,
            String name,
            String description,
            TopicCategory category,
            String creatorId
    ) {
        create(topicId, name, description, category, creatorId).join();
    }
}

