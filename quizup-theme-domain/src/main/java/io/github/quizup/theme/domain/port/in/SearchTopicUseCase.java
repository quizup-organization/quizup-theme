package io.github.quizup.theme.domain.port.in;

import io.github.quizup.common.domain.model.search.*;
import io.github.quizup.theme.domain.model.Topic;
import io.github.quizup.theme.domain.query.TopicQuery;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface SearchTopicUseCase {

    CompletableFuture<PageResult<Topic>> search(TopicQuery.TopicSearchQuery query);

    default CompletableFuture<PageResult<Topic>> search(List<FilterCriteria> filters,
                                                        List<SortCriteria> sorts,
                                                        PageCriteria page) {
        return search(
                new TopicQuery.TopicSearchQuery(
                        filters,
                        sorts,
                        page
                )
        );
    }
}

