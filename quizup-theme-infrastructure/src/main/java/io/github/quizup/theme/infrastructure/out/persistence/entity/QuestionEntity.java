package io.github.quizup.theme.infrastructure.out.persistence.entity;

import io.github.quizup.microservice.core.domain.model.search.FieldType;
import io.github.quizup.microservice.core.domain.model.search.Searchable;
import io.github.quizup.theme.domain.model.QuestionChoice;
import io.github.quizup.theme.domain.model.QuestionDifficulty;
import io.github.quizup.theme.domain.model.QuestionStatus;
import io.github.quizup.theme.domain.model.QuestionSuspensionReason;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * QuestionEntity - Entité JPA de la projection read-only des questions (métadonnées seules ;
 * les contenus localisés vivent dans {@code question_content}).
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

    /** Illustration optionnelle : URL externe (http/https), longueur max 1024. */
    @Column(name = "image_url", length = 1024)
    private String imageUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "correct_answer", nullable = false)
    private QuestionChoice correctAnswer;

    @Searchable(type = FieldType.STRING)
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private QuestionStatus status;

    /** Difficulté déduite du taux de bonnes réponses (null tant que l'échantillon est insuffisant). */
    @Searchable(type = FieldType.STRING)
    @Enumerated(EnumType.STRING)
    @Column(name = "difficulty")
    private QuestionDifficulty difficulty;

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

    @Column(name = "suspended", nullable = false)
    private boolean suspended;

    @Enumerated(EnumType.STRING)
    @Column(name = "suspension_reason", length = 40)
    private QuestionSuspensionReason suspensionReason;

    @Column(name = "suspension_note", length = 500)
    private String suspensionNote;

    @Column(name = "suspended_at")
    private Instant suspendedAt;

    @Column(name = "suspended_by", length = 255)
    private String suspendedBy;

    @Column(name = "reinstated_at")
    private Instant reinstatedAt;

    @Column(name = "archived", nullable = false)
    private boolean archived;

    @Column(name = "archive_note", length = 500)
    private String archiveNote;

    @Column(name = "archived_at")
    private Instant archivedAt;

    @Column(name = "archived_by", length = 255)
    private String archivedBy;

    @Column(name = "restored_at")
    private Instant restoredAt;

}
