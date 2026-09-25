package io.github.quizup.theme.domain.query;

import io.github.quizup.microservice.core.infrastructure.in.api.request.SearchRequest;

public interface TopicQuery {

    record TopicSearchQuery(SearchRequest request) implements TopicQuery {
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
