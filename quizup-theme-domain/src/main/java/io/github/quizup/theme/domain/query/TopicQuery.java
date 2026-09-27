package io.github.quizup.theme.domain.query;

import io.github.quizup.microservice.core.infrastructure.in.api.request.SearchRequest;
import io.github.quizup.theme.domain.model.TopicCategory;
import io.github.quizup.theme.domain.model.TopicSort;

import java.util.List;

public interface TopicQuery {

    record TopicSearchQuery(SearchRequest request) implements TopicQuery {
    }

    /**
     * Page du catalogue publié. {@code nameQuery} est le texte brut (normalisé par le handler) ;
     * {@code category} et le tri sont optionnels ({@code null} = pas de filtre / tri par défaut).
     */
    record GetTopicPageQuery(
            String nameQuery,
            TopicCategory category,
            TopicSort sort,
            int page,
            int size
    ) implements TopicQuery {
    }

    /**
     * Facettes du catalogue : compteurs par catégorie pour les filtres courants.
     * {@code nameQuery} est le texte brut (normalisé par le handler) ; {@code topicIds}
     * restreint éventuellement le périmètre (ex. sujets suivis), {@code null} = aucune restriction.
     */
    record TopicFacetsQuery(String nameQuery, List<String> topicIds) implements TopicQuery {
    }

    /**
     * Récupération batch de thèmes ; l'ordre demandé est préservé.
     */
    record GetTopicsByIdsQuery(List<String> topicIds) implements TopicQuery {
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
