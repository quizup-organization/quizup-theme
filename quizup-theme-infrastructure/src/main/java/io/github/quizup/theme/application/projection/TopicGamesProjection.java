package io.github.quizup.theme.application.projection;

import io.github.quizup.game.domain.event.GameEvent;
import io.github.quizup.theme.domain.port.out.TopicPlayedGameRefRepositoryPort;
import io.github.quizup.theme.domain.port.out.TopicRepositoryPort;
import org.axonframework.config.ProcessingGroup;
import org.axonframework.eventhandling.EventHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Compteur de **parties jouées** par thème ({@code topic_entry.games_counter}), qui alimente le
 * tri catalogue « Les plus joués » ({@code TopicSort.MOST_PLAYED}).
 *
 * <p>Consomme {@link GameEvent.GameCreatedEvent} du service {@code quizup-game} : chaque partie
 * est d'abord **référencée** (clé naturelle {@code gameId}, donc un rejeu ne la compte pas deux
 * fois), puis le compteur est **recalculé** depuis cette référence — même modèle que
 * {@code followersCounter} (`TopicProjection`). Compteur communautaire : toutes les parties
 * comptent, bots compris, et il ne dépend d'aucun joueur particulier.</p>
 *
 * <p>Une partie devancée par la projection de son thème (flux distincts, aucun ordre entre les
 * deux) est référencée mais pas comptée tout de suite : {@code TopicProjection} initialise
 * {@code gamesCounter} à la création du thème, ce qui rattrape ce cas.</p>
 */
@Component
@ProcessingGroup("theme-projection")
public class TopicGamesProjection {

    private final TopicRepositoryPort topicRepositoryPort;
    private final TopicPlayedGameRefRepositoryPort playedGameRefRepositoryPort;

    public TopicGamesProjection(TopicRepositoryPort topicRepositoryPort,
                                TopicPlayedGameRefRepositoryPort playedGameRefRepositoryPort) {
        this.topicRepositoryPort = topicRepositoryPort;
        this.playedGameRefRepositoryPort = playedGameRefRepositoryPort;
    }

    @EventHandler
    @Transactional
    public void on(GameEvent.GameCreatedEvent event) {
        if (event.gameId() == null || event.topicId() == null) {
            return;
        }
        playedGameRefRepositoryPort.record(event.gameId(), event.topicId());
        topicRepositoryPort.findById(event.topicId())
                .ifPresent(topic -> topicRepositoryPort.save(topic.toBuilder()
                        .gamesCounter(playedGameRefRepositoryPort.countByTopicId(event.topicId()))
                        .build()));
    }
}
