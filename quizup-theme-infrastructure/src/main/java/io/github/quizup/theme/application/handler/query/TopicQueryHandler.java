package io.github.quizup.theme.application.handler.query;

import io.github.quizup.common.domain.model.search.PageResult;
import io.github.quizup.theme.domain.exception.TopicProblems;
import io.github.quizup.theme.domain.model.Topic;
import io.github.quizup.theme.domain.port.out.TopicRepositoryPort;
import io.github.quizup.theme.domain.query.TopicQuery;
import org.axonframework.queryhandling.QueryHandler;
import org.springframework.stereotype.Component;

@Component
public class TopicQueryHandler {

    private final TopicRepositoryPort topicRepositoryPort;

    public TopicQueryHandler(TopicRepositoryPort topicRepositoryPort) {
        this.topicRepositoryPort = topicRepositoryPort;
    }

    @QueryHandler
    public PageResult<Topic> handle(TopicQuery.TopicSearchQuery query) {
        return topicRepositoryPort.findAll(query);
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
}
