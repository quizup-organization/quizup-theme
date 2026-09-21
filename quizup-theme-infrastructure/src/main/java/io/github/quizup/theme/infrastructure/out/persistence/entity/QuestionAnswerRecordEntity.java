package io.github.quizup.theme.infrastructure.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Journal des réponses humaines déjà comptées pour la difficulté d'une question.
 * La clé métier {@code (game_id, round, player_id)} rend la projection idempotente au rejeu.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "question_answer_record")
public class QuestionAnswerRecordEntity {

    @EmbeddedId
    private QuestionAnswerRecordId id;

    @Column(name = "question_id", length = 255, nullable = false)
    private String questionId;

    @Column(name = "correct", nullable = false)
    private boolean correct;

    @Column(name = "answered_at", nullable = false)
    private Instant answeredAt;

    public QuestionAnswerRecordEntity(QuestionAnswerRecordId id, String questionId, boolean correct, Instant answeredAt) {
        this.id = id;
        this.questionId = questionId;
        this.correct = correct;
        this.answeredAt = answeredAt;
    }
}
