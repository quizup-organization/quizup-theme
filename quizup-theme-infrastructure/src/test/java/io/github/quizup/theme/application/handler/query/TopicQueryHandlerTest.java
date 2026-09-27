package io.github.quizup.theme.application.handler.query;

import io.github.quizup.theme.domain.model.Topic;
import io.github.quizup.theme.domain.model.TopicCategory;
import io.github.quizup.theme.domain.model.TopicFacetCount;
import io.github.quizup.theme.domain.port.out.TopicRepositoryPort;
import io.github.quizup.theme.domain.query.TopicQuery;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TopicQueryHandlerTest {

    private final TopicRepositoryPort repository = mock(TopicRepositoryPort.class);
    private final TopicQueryHandler handler = new TopicQueryHandler(repository);

    @Test
    void facets_normalizes_text_and_passes_scope() {
        when(repository.countByCategory("poke", List.of("t1")))
                .thenReturn(List.of(new TopicFacetCount(TopicCategory.GAMES, 2)));

        List<TopicFacetCount> facets = handler.handle(
                new TopicQuery.TopicFacetsQuery("  Poké  ", List.of("t1")));

        assertThat(facets).containsExactly(new TopicFacetCount(TopicCategory.GAMES, 2));
        verify(repository).countByCategory("poke", List.of("t1"));
    }

    @Test
    void facets_treats_blank_text_as_no_filter() {
        when(repository.countByCategory(null, null)).thenReturn(List.of());

        handler.handle(new TopicQuery.TopicFacetsQuery("   ", null));

        verify(repository).countByCategory(null, null);
    }

    @Test
    void topics_by_ids_delegates_to_repository() {
        Topic topic = Topic.builder().topicId("t1").name("Pokémon").build();
        when(repository.findAllByIds(List.of("t1"))).thenReturn(List.of(topic));

        assertThat(handler.handle(new TopicQuery.GetTopicsByIdsQuery(List.of("t1"))))
                .containsExactly(topic);
    }
}
