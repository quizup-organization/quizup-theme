package io.github.quizup.theme.domain.port.in;

import io.github.quizup.microservice.core.infrastructure.in.api.request.SearchRequest;
import io.github.quizup.microservice.core.infrastructure.in.api.response.SearchResponse;
import io.github.quizup.theme.domain.model.Topic;
import io.github.quizup.theme.domain.query.TopicQuery;

import java.util.concurrent.CompletableFuture;

public interface SearchTopicUseCase {

    CompletableFuture<SearchResponse<Topic>> search(TopicQuery.TopicSearchQuery query);

    default CompletableFuture<SearchResponse<Topic>> search(SearchRequest request) {
        return search(new TopicQuery.TopicSearchQuery(request));
    }
}
