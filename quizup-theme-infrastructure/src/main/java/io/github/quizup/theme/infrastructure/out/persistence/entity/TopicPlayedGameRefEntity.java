package io.github.quizup.theme.infrastructure.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Parties connues par thème (clé naturelle {@code game_id}). Source du recalcul de
 * {@code topic_entry.games_counter} (idempotent/rejouable).
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "topic_played_game_ref")
public class TopicPlayedGameRefEntity {

    @Id
    @Column(name = "game_id", length = 255, nullable = false)
    private String gameId;

    @Column(name = "topic_id", length = 255, nullable = false)
    private String topicId;

    public TopicPlayedGameRefEntity(String gameId, String topicId) {
        this.gameId = gameId;
        this.topicId = topicId;
    }
}
