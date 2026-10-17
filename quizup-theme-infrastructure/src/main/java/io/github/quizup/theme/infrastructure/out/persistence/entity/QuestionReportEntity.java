package io.github.quizup.theme.infrastructure.out.persistence.entity;

import io.github.quizup.theme.domain.model.QuestionReportReason;
import io.github.quizup.theme.domain.model.QuestionReportSnapshotSource;
import io.github.quizup.theme.domain.model.QuestionReportStatus;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

/** Projection read-only d'un signalement de question. */
@Setter
@Getter
@Entity
@Table(name = "question_report", indexes = {
        @Index(name = "idx_question_report_question", columnList = "question_id,status"),
        @Index(name = "idx_question_report_status", columnList = "status,created_at"),
        @Index(name = "idx_question_report_thread", columnList = "thread_id")
})
public class QuestionReportEntity {

    @Id
    @Column(name = "report_id", length = 255, nullable = false)
    private String reportId;

    @Column(name = "thread_id", length = 255, nullable = false)
    private String threadId;

    @Column(name = "question_id", length = 255, nullable = false)
    private String questionId;

    @Column(name = "topic_id", length = 255, nullable = false)
    private String topicId;

    @Column(name = "reporter_id", length = 255, nullable = false)
    private String reporterId;

    @Column(name = "game_id", length = 255)
    private String gameId;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "question_report_reason", joinColumns = @JoinColumn(name = "report_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "reason", length = 32, nullable = false)
    private Set<QuestionReportReason> reasons = new HashSet<>();

    @Column(name = "comment", length = 500)
    private String comment;

    @Column(name = "language", length = 5)
    private String language;

    @Column(name = "snapshot_json", columnDefinition = "TEXT")
    private String snapshotJson;

    @Enumerated(EnumType.STRING)
    @Column(name = "snapshot_source", length = 20)
    private QuestionReportSnapshotSource snapshotSource;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private QuestionReportStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "reviewed_by", length = 255)
    private String reviewedBy;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @Column(name = "resolution_note", length = 500)
    private String resolutionNote;
}
