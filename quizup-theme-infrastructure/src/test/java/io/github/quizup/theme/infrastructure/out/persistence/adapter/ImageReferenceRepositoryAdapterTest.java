package io.github.quizup.theme.infrastructure.out.persistence.adapter;

import io.github.quizup.theme.domain.model.ImageReferenceCount;
import io.github.quizup.theme.infrastructure.out.persistence.repository.QuestionJpaRepository;
import io.github.quizup.theme.infrastructure.out.persistence.repository.TopicJpaRepository;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ImageReferenceRepositoryAdapterTest {

    private final TopicJpaRepository topicJpaRepository = mock(TopicJpaRepository.class);
    private final QuestionJpaRepository questionJpaRepository = mock(QuestionJpaRepository.class);
    private final ImageReferenceRepositoryAdapter adapter =
            new ImageReferenceRepositoryAdapter(topicJpaRepository, questionJpaRepository);

    @Test
    void counts_sum_topic_cover_banner_and_question_illustrations() {
        List<String> urls = List.of("u1", "u2", "u3");
        when(topicJpaRepository.countByImageUrlIn(urls)).thenReturn(List.<Object[]>of(new Object[]{"u1", 2L}));
        when(topicJpaRepository.countByBannerUrlIn(urls)).thenReturn(List.<Object[]>of(new Object[]{"u1", 1L}));
        when(questionJpaRepository.countByImageUrlIn(urls))
                .thenReturn(List.<Object[]>of(new Object[]{"u2", 4L}, new Object[]{"u3", 1L}));

        assertThat(adapter.countByImageUrls(urls)).containsExactly(
                new ImageReferenceCount("u1", 3),
                new ImageReferenceCount("u2", 4),
                new ImageReferenceCount("u3", 1));
    }

    @Test
    void returns_empty_list_without_querying_for_empty_input() {
        assertThat(adapter.countByImageUrls(List.of())).isEmpty();
        assertThat(adapter.countByImageUrls(null)).isEmpty();
        verifyNoInteractions(topicJpaRepository, questionJpaRepository);
    }
}
