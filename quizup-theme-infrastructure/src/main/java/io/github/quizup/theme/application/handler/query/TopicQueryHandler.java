package io.github.quizup.theme.application.handler.query;

import io.github.quizup.microservice.core.infrastructure.in.api.response.SearchResponse;
import io.github.quizup.theme.domain.exception.TopicProblems;
import io.github.quizup.theme.domain.model.Topic;
import io.github.quizup.theme.domain.model.TopicFacetCount;
import io.github.quizup.theme.domain.model.TopicPage;
import io.github.quizup.theme.domain.model.TopicSort;
import io.github.quizup.theme.domain.port.out.TopicRepositoryPort;
import io.github.quizup.theme.domain.query.TopicQuery;
import io.github.quizup.theme.domain.util.SearchText;
import org.axonframework.queryhandling.QueryHandler;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class TopicQueryHandler {

    private final TopicRepositoryPort topicRepositoryPort;

    public TopicQueryHandler(TopicRepositoryPort topicRepositoryPort) {
        this.topicRepositoryPort = topicRepositoryPort;
    }

    @QueryHandler
    public SearchResponse<Topic> handle(TopicQuery.TopicSearchQuery query) {
        return topicRepositoryPort.findAll(query.request());
    }

    @QueryHandler
    public Topic handle(TopicQuery.GetTopicByIdQuery query) {
        return topicRepositoryPort.findById(query.topicId())
                .orElseThrow(() -> new TopicProblems.TopicNotFoundProblem(query.topicId()));
    }

    @QueryHandler
    public boolean handle(TopicQuery.TopicExistsByIdQuery query) {
        return topicRepositoryPort.existsById(query.topicId());
    }

    @QueryHandler
    public List<TopicFacetCount> handle(TopicQuery.TopicFacetsQuery query) {
        String normalizedName = normalize(query.nameQuery());
        return topicRepositoryPort.countByCategory(normalizedName, query.topicIds());
    }

    @QueryHandler
    public TopicPage handle(TopicQuery.GetTopicPageQuery query) {
        return topicRepositoryPort.findPage(
                normalize(query.nameQuery()),
                query.category(),
                query.sort() == null ? TopicSort.POPULAR : query.sort(),
                query.page(),
                query.size());
    }

    @QueryHandler
    public List<Topic> handle(TopicQuery.GetTopicsByIdsQuery query) {
        return topicRepositoryPort.findAllByIds(query.topicIds());
    }

    @QueryHandler
    public TopicPage handle(TopicQuery.GetTopicsByCreatorQuery query) {
        return topicRepositoryPort.findByCreatorId(query.creatorId(), query.page(), query.size());
    }

    private static String normalize(String nameQuery) {
        return nameQuery == null || nameQuery.isBlank() ? null : SearchText.normalize(nameQuery);
    }
}
