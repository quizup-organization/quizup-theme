package io.github.quizup.theme.infrastructure.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

/** Clé composite {@code (game_id, round, player_id)} d'une réponse comptée. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@Embeddable
public class QuestionAnswerRecordId implements Serializable {

    @Column(name = "game_id", length = 255, nullable = false)
    private String gameId;

    @Column(name = "round", length = 32, nullable = false)
    private String round;

    @Column(name = "player_id", length = 255, nullable = false)
    private String playerId;
}
