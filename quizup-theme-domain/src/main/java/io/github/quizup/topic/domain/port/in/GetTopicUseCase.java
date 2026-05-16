package io.github.quizup.topic.domain.port.in;

import io.github.quizup.topic.domain.exception.TopicProblems;
import io.github.quizup.topic.domain.model.Topic;
import io.github.quizup.topic.domain.query.TopicQuery;

import java.util.concurrent.CompletableFuture;

public interface GetTopicUseCase {
    CompletableFuture<Topic> getById(TopicQuery.GetTopicByIdQuery query) throws TopicProblems.TopicNotFoundProblem;

    default CompletableFuture<Topic> getById(String topicId) throws TopicProblems.TopicNotFoundProblem {
        return getById(
                new TopicQuery.GetTopicByIdQuery(
                        topicId
                )
        );
    }
}

