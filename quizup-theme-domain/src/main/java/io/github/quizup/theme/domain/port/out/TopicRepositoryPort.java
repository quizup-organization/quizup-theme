package io.github.quizup.theme.domain.port.out;

import io.github.quizup.microservice.core.infrastructure.in.api.request.SearchRequest;
import io.github.quizup.microservice.core.infrastructure.in.api.response.SearchResponse;
import io.github.quizup.theme.domain.model.Topic;
import io.github.quizup.theme.domain.model.TopicCategory;
import io.github.quizup.theme.domain.model.TopicFacetCount;
import io.github.quizup.theme.domain.model.TopicPage;
import io.github.quizup.theme.domain.model.TopicSort;

import java.util.List;
import java.util.Optional;

public interface TopicRepositoryPort {
    void save(Topic topic);
    Optional<Topic> findById(String topicId);
    boolean existsById(String topicId);
    SearchResponse<Topic> findAll(SearchRequest request);

    /**
     * Compteurs de sujets publiés par catégorie.
     *
     * @param normalizedName filtre texte normalisé ({@code null} = pas de filtre)
     * @param topicIds       restriction de périmètre ({@code null} = aucune ; liste vide = aucun résultat)
     */
    List<TopicFacetCount> countByCategory(String normalizedName, List<String> topicIds);

    /**
     * Page du catalogue publié, filtrée par texte et catégorie.
     *
     * @param normalizedName filtre texte normalisé ({@code null} = pas de filtre)
     * @param category       catégorie ({@code null} = toutes)
     * @param sort           tri demandé ({@code null} = {@link TopicSort#POPULAR})
     */
    TopicPage findPage(String normalizedName, TopicCategory category, TopicSort sort, int page, int size);

    /**
     * Récupération batch ; l'ordre demandé est préservé, les ids inconnus sont ignorés.
     */
    List<Topic> findAllByIds(List<String> topicIds);
}
