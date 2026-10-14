package io.github.quizup.theme.application.projection;

import io.github.quizup.game.domain.event.GameEvent;
import io.github.quizup.game.domain.model.GamePlayerType;
import io.github.quizup.theme.domain.model.Topic;
import io.github.quizup.theme.domain.port.out.TopicPlayedGameRefRepositoryPort;
import io.github.quizup.theme.domain.port.out.TopicRepositoryPort;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TopicGamesProjectionTest {

    private static final Instant AT = Instant.parse("2026-10-14T12:00:00Z");

    private final TopicRepositoryPort topicRepository = mock(TopicRepositoryPort.class);
    private final TopicPlayedGameRefRepositoryPort playedGameRefRepository =
            mock(TopicPlayedGameRefRepositoryPort.class);
    private final TopicGamesProjection projection = new TopicGamesProjection(
            topicRepository, playedGameRefRepository);

    private static GameEvent.GameCreatedEvent gameCreated(String gameId, String topicId) {
        return new GameEvent.GameCreatedEvent(
                gameId,
                topicId,
                "player-1",
                "Alice",
                "player-2",
                "Bob",
                GamePlayerType.BOT,
                List.of(),
                null,
                null,
                null,
                AT);
    }

    @Test
    void gameCreated_referencesTheGameAndRecomputesTheTopicCounter() {
        when(topicRepository.findById("topic-1"))
                .thenReturn(Optional.of(Topic.builder().topicId("topic-1").gamesCounter(1).build()));
        when(playedGameRefRepository.countByTopicId("topic-1")).thenReturn(2);

        projection.on(gameCreated("game-1", "topic-1"));

        verify(playedGameRefRepository).record("game-1", "topic-1");
        ArgumentCaptor<Topic> captor = ArgumentCaptor.forClass(Topic.class);
        verify(topicRepository).save(captor.capture());
        assertThat(captor.getValue().topicId()).isEqualTo("topic-1");
        assertThat(captor.getValue().gamesCounter()).isEqualTo(2);
    }

    @Test
    void gameCreated_unknownTopic_stillReferencesTheGameWithoutTouchingTheReadModel() {
        // Le thème n'est pas encore projeté (flux distincts) : la partie est référencée et le
        // compteur sera rattrapé à la création du thème.
        when(topicRepository.findById("topic-2")).thenReturn(Optional.empty());

        projection.on(gameCreated("game-2", "topic-2"));

        verify(playedGameRefRepository).record("game-2", "topic-2");
        verify(topicRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void gameCreated_withoutTopic_isIgnored() {
        projection.on(gameCreated("game-3", null));

        verify(playedGameRefRepository, never()).record(org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any());
        verify(topicRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }
}
