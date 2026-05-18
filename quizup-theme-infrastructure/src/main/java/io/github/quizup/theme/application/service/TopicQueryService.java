package io.github.quizup.theme.application.service;

import io.github.quizup.common.domain.model.search.PageResult;
import io.github.quizup.common.infrastructure.axon.PageResponseTypes;
import io.github.quizup.theme.domain.exception.TopicProblems;
import io.github.quizup.theme.domain.model.Topic;
import io.github.quizup.theme.domain.port.in.CheckTopicUseCase;
import io.github.quizup.theme.domain.port.in.GetTopicUseCase;
import io.github.quizup.theme.domain.port.in.SearchTopicUseCase;
import io.github.quizup.theme.domain.query.TopicQuery;
import org.axonframework.messaging.responsetypes.ResponseTypes;
import org.axonframework.queryhandling.QueryGateway;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
public class TopicQueryService implements GetTopicUseCase, SearchTopicUseCase, CheckTopicUseCase {

    private final QueryGateway queryGateway;

    public TopicQueryService(QueryGateway queryGateway) {
        this.queryGateway = queryGateway;
    }

    @Override
    public CompletableFuture<Topic> getById(TopicQuery.GetTopicByIdQuery query) throws TopicProblems.TopicNotFoundProblem {
        return queryGateway.query(query, ResponseTypes.instanceOf(Topic.class));
    }

    @Override
    public CompletableFuture<Boolean> existsById(TopicQuery.TopicExistsByIdQuery query) {
        return queryGateway.query(query, ResponseTypes.instanceOf(Boolean.class));
    }

    @Override
    public CompletableFuture<PageResult<Topic>> search(TopicQuery.TopicSearchQuery query) {
        return queryGateway.query(query, PageResponseTypes.pageResultOf(Topic.class));
    }
}

