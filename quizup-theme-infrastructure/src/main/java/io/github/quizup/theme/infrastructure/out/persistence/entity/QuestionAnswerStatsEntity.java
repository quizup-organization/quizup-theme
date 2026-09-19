package io.github.quizup.theme.infrastructure.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * Compteurs de réponses d'une question (base du calcul de difficulté), hors agrégat :
 * projection alimentée par les réponses des parties humaines.
 */
@Setter
@Getter
@Entity
@Table(name = "question_answer_stats")
public class QuestionAnswerStatsEntity {

    @Id
    @Column(name = "question_id", length = 255, nullable = false)
    private String questionId;

    @Column(name = "answer_count", nullable = false)
    private int answerCount;

    @Column(name = "correct_count", nullable = false)
    private int correctCount;
}
