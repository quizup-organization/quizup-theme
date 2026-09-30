package io.github.quizup.theme.infrastructure.out.persistence.entity;

import io.github.quizup.theme.domain.model.QuestionChoice;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.MapKeyEnumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

/**
 * Traduction d'une question (hors langue source) : texte + réponses A-D.
 * La langue source vit dans {@code question_entry} / {@code question_answer_entry}.
 */
@Setter
@Getter
@Entity
@Table(name = "question_translation", uniqueConstraints = {
        @UniqueConstraint(name = "uq_question_translation", columnNames = {"question_id", "language"})
})
public class QuestionTranslationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "question_id", length = 255, nullable = false)
    private String questionId;

    /** Code ISO 639-1 (fr, en). */
    @Column(name = "language", length = 5, nullable = false)
    private String language;

    @Column(name = "text", length = 255, nullable = false)
    private String text;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "question_answer_translation",
            joinColumns = @JoinColumn(name = "translation_id"))
    @MapKeyEnumerated(jakarta.persistence.EnumType.STRING)
    @MapKeyColumn(name = "choice")
    @Column(name = "answer_text", length = 255, nullable = false)
    private Map<QuestionChoice, String> answers = new HashMap<>();
}
