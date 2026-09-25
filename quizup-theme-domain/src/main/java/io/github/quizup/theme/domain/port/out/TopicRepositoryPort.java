package io.github.quizup.theme.domain.port.out;

import io.github.quizup.microservice.core.infrastructure.in.api.request.SearchRequest;
import io.github.quizup.microservice.core.infrastructure.in.api.response.SearchResponse;
import io.github.quizup.theme.domain.model.Topic;

import java.util.Optional;

public interface TopicRepositoryPort {
    void save(Topic topic);
    Optional<Topic> findById(String topicId);
    boolean existsById(String topicId);
    SearchResponse<Topic> findAll(SearchRequest request);
}
