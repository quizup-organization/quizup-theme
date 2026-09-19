package io.github.quizup.theme.domain.port.in;

import io.github.quizup.theme.domain.query.TopicQuery;

import java.util.concurrent.CompletableFuture;

public interface CheckTopicUseCase {

    CompletableFuture<Boolean> existsById(TopicQuery.TopicExistsByIdQuery query);

    default CompletableFuture<Boolean> existsById(String topicId) {
        return existsById(
                new TopicQuery.TopicExistsByIdQuery(
                        topicId
                )
        );
    }

    default Boolean existsByIdAndWait(String topicId) {
        return existsById(topicId).join();
    }
}

