package io.github.quizup.topic.domain.query;

import io.github.quizup.common.domain.model.search.FilterCriteria;
import io.github.quizup.common.domain.model.search.PageCriteria;
import io.github.quizup.common.domain.model.search.SortCriteria;
import io.github.quizup.common.domain.query.SearchQuery;
import io.github.quizup.topic.domain.model.TopicCategory;
import io.github.quizup.topic.domain.model.TopicStatus;

import java.util.List;

public interface TopicQuery {

    record TopicSearchQuery(
            List<FilterCriteria> filters,
            List<SortCriteria> sorts,
            PageCriteria page
    ) implements TopicQuery, SearchQuery {

    }

    /**
     * Query pour vérifier l'existence d'un thème
     */
    record TopicExistsByIdQuery(
            String topicId
    ) {
    }

    /**
     * Query pour récupérer un thème par son ID
     */
    record GetTopicByIdQuery(
            String topicId
    ) {
    }
}
