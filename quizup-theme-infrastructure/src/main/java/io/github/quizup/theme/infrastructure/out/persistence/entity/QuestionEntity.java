package io.github.quizup.theme.infrastructure.out.persistence.entity;

import io.github.quizup.common.domain.model.search.FieldType;
import io.github.quizup.common.domain.model.search.Searchable;
import io.github.quizup.theme.domain.model.QuestionChoice;
import io.github.quizup.theme.domain.model.QuestionStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * QuestionEntity - Entité JPA pour la projection read-only des questions
 * Mise à jour via les Event Handlers (projection)
 */
@Setter
@Getter
@Entity
@Table(name = "question_entry", indexes = {
        @Index(name = "idx_question_entry_topic", columnList = "topic_id"),
        @Index(name = "idx_question_entry_status", columnList = "status"),
        @Index(name = "idx_question_entry_creator", columnList = "creator_id")
})
public class QuestionEntity {

    @Id
    @Searchable(type = FieldType.STRING)
    @Column(name = "question_id", length = 255, nullable = false)
    private String questionId;

    @Searchable(type = FieldType.STRING)
    @Column(name = "topic_id", length = 255, nullable = false)
    private String topicId;

    @Column(name = "text", length = 255, nullable = false)
    private String text;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "question_answer_entry", joinColumns = @JoinColumn(name = "question_id"))
    @MapKeyEnumerated(EnumType.STRING)
    @MapKeyColumn(name = "choice")
    @Column(name = "answer_text", length = 255, nullable = false)
    private Map<QuestionChoice, String> answers = new HashMap<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "correct_answer", nullable = false)
    private QuestionChoice correctAnswer;

    @Searchable(type = FieldType.STRING)
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private QuestionStatus status;

    @Searchable(type = FieldType.STRING)
    @Column(name = "creator_id", length = 255, nullable = false)
    private String creatorId;

    @Searchable(type = FieldType.DATE)
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Searchable(type = FieldType.STRING)
    @Column(name = "updated_by", length = 255)
    private String updatedBy;

    @Searchable(type = FieldType.DATE)
    @Column(name = "updated_at")
    private Instant updatedAt;

}

