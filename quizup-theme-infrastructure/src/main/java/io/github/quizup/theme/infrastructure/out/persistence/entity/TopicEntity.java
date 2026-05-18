package io.github.quizup.theme.infrastructure.out.persistence.entity;

import io.github.quizup.common.domain.model.search.FieldType;
import io.github.quizup.common.domain.model.search.Searchable;
import io.github.quizup.theme.domain.model.TopicCategory;
import io.github.quizup.theme.domain.model.TopicStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * TopicEntity - Entité JPA pour la projection read-only du thème
 * Mise à jour via les Event Handlers (projection)
 */
@Setter
@Getter
@Entity
@Table(name = "topic_entry", indexes = {
        @Index(name = "idx_topic_entry_creator", columnList = "creator_id"),
        @Index(name = "idx_topic_entry_status", columnList = "status"),
        @Index(name = "idx_topic_entry_category", columnList = "category")
})
public class TopicEntity {

    @Id
    @Searchable(type = FieldType.STRING)
    @Column(name = "topic_id", length = 255, nullable = false)
    private String topicId;

    @Searchable(type = FieldType.STRING)
    @Column(name = "name", length = 25, nullable = false)
    private String name;

    @Searchable(type = FieldType.STRING)
    @Column(name = "description", length = 500)
    private String description;

    @Searchable(type = FieldType.STRING)
    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false)
    private TopicCategory category;

    @Searchable(type = FieldType.STRING)
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private TopicStatus status;

    @Searchable(type = FieldType.STRING)
    @Column(name = "creator_id", length = 255, nullable = false)
    private String creatorId;

    @Searchable(type = FieldType.NUMBER)
    @Column(name = "question_count", nullable = false)
    private int questionCount = 0;

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

