package io.github.quizup.theme.domain.port.in;

import io.github.quizup.theme.domain.command.TopicCommand;

import java.util.concurrent.CompletableFuture;

public interface PublishTopicUseCase {
    CompletableFuture<String> publish(TopicCommand.PublishTopicCommand command);

    default CompletableFuture<String> publish(String topicId, String requesterId) {
        return publish(
                new TopicCommand.PublishTopicCommand(
                        topicId,
                        requesterId
                )
        );
    }

    default void publishAndWait(String topicId, String requesterId) {
        publish(topicId, requesterId).join();
    }
}

