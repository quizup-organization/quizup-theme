package io.github.quizup.theme.application.projection;

import io.github.quizup.microservice.core.domain.model.i18n.Language;
import io.github.quizup.theme.domain.event.QuestionEvent;
import io.github.quizup.theme.domain.model.Question;
import io.github.quizup.theme.domain.model.Topic;
import io.github.quizup.theme.domain.port.out.QuestionRepositoryPort;
import io.github.quizup.theme.domain.port.out.TopicFollowerRefRepositoryPort;
import io.github.quizup.theme.domain.port.out.TopicPlayedGameRefRepositoryPort;
import io.github.quizup.theme.domain.port.out.TopicRepositoryPort;
import org.axonframework.eventhandling.EventHandler;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.Arrays;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TopicProjectionTest {

    private static final Instant AT = Instant.parse("2026-10-11T12:00:00Z");

    private final TopicRepositoryPort topicRepository = mock(TopicRepositoryPort.class);
    private final TopicFollowerRefRepositoryPort followerRefRepository =
            mock(TopicFollowerRefRepositoryPort.class);
    private final TopicPlayedGameRefRepositoryPort playedGameRefRepository =
            mock(TopicPlayedGameRefRepositoryPort.class);
    private final QuestionRepositoryPort questionRepository = mock(QuestionRepositoryPort.class);
    private final TopicProjection projection = new TopicProjection(
            topicRepository, followerRefRepository, playedGameRefRepository, questionRepository);

    @Test
    void questionTextUpdate_bumpsTopicUpdatedAt() {
        when(questionRepository.findById("question-1"))
                .thenReturn(Optional.of(Question.builder().questionId("question-1").topicId("topic-1").build()));
        when(topicRepository.findById("topic-1"))
                .thenReturn(Optional.of(Topic.builder().topicId("topic-1").build()));

        projection.on(new QuestionEvent.QuestionTextUpdatedEvent(
                "question-1", Language.FR, "Nouveau texte", "author-1", AT));

        ArgumentCaptor<Topic> captor = ArgumentCaptor.forClass(Topic.class);
        verify(topicRepository).save(captor.capture());
        assertThat(captor.getValue().topicId()).isEqualTo("topic-1");
        assertThat(captor.getValue().updatedAt()).isEqualTo(AT);
    }

    @Test
    void questionTranslationAdded_bumpsTopicUpdatedAt() {
        when(questionRepository.findById("question-1"))
                .thenReturn(Optional.of(Question.builder().questionId("question-1").topicId("topic-1").build()));
        when(topicRepository.findById("topic-1"))
                .thenReturn(Optional.of(Topic.builder().topicId("topic-1").build()));

        projection.on(new QuestionEvent.QuestionTranslationAddedEvent(
                "question-1", Language.EN, "New text", java.util.Map.of(), "author-1", AT));

        ArgumentCaptor<Topic> captor = ArgumentCaptor.forClass(Topic.class);
        verify(topicRepository).save(captor.capture());
        assertThat(captor.getValue().updatedAt()).isEqualTo(AT);
    }

    @Test
    void difficultyUpdate_isNotHandled() {
        boolean handled = Arrays.stream(TopicProjection.class.getMethods())
                .filter(method -> method.isAnnotationPresent(EventHandler.class))
                .anyMatch(method -> method.getParameterTypes()[0]
                        .equals(QuestionEvent.QuestionDifficultyUpdatedEvent.class));

        assertThat(handled).isFalse();
    }

    @Test
    void topicCreated_initialisesGamesCounterFromKnownReferences() {
        // Une partie peut précéder la projection de son thème (flux distincts) : les références
        // déjà connues sont rattrapées à la création.
        when(playedGameRefRepository.countByTopicId("topic-1")).thenReturn(2);

        projection.on(new io.github.quizup.theme.domain.event.TopicEvent.TopicCreatedEvent(
                "topic-1", java.util.Map.of(Language.FR, "Sujet"), "desc",
                io.github.quizup.theme.domain.model.TopicCategory.GENERAL,
                null, null, null, "creator-1", AT));

        ArgumentCaptor<Topic> captor = ArgumentCaptor.forClass(Topic.class);
        verify(topicRepository).save(captor.capture());
        assertThat(captor.getValue().gamesCounter()).isEqualTo(2);
        assertThat(captor.getValue().followersCounter()).isZero();
    }
}
