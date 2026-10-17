package io.github.quizup.theme.domain.aggregate;

import io.github.quizup.theme.domain.command.QuestionReportCommand;
import io.github.quizup.theme.domain.event.QuestionReportEvent;
import io.github.quizup.theme.domain.exception.QuestionReportProblems;
import io.github.quizup.theme.domain.model.QuestionReportResult;
import io.github.quizup.theme.domain.model.QuestionReportRules;
import io.github.quizup.theme.domain.model.QuestionReportStatus;
import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.eventsourcing.EventSourcingHandler;
import org.axonframework.modelling.command.AggregateCreationPolicy;
import org.axonframework.modelling.command.AggregateIdentifier;
import org.axonframework.modelling.command.AggregateLifecycle;
import org.axonframework.modelling.command.CreationPolicy;
import org.axonframework.spring.stereotype.Aggregate;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Thread de signalements d'un joueur sur une question.
 *
 * <p>L'identifiant est déterministe ({@link QuestionReportRules#threadId}) : toutes les
 * commandes d'un même couple joueur × question atterrissent sur le même agrégat. Un seul
 * signalement peut être {@code OPEN} à la fois ; après clôture, un nouveau signalement peut être
 * déposé et l'historique est conservé dans le read model.</p>
 */
@Aggregate
public class QuestionReportThreadAggregate {

    @AggregateIdentifier
    private String threadId;
    private String questionId;
    private String reporterId;
    private final Map<String, QuestionReportStatus> reports = new HashMap<>();
    private String currentOpenReportId;

    protected QuestionReportThreadAggregate() {
    }

    @CommandHandler
    @CreationPolicy(AggregateCreationPolicy.CREATE_IF_MISSING)
    public QuestionReportResult handle(QuestionReportCommand.ReportQuestionCommand command) {
        if (currentOpenReportId != null) {
            return new QuestionReportResult(currentOpenReportId, false);
        }
        try {
            QuestionReportRules.validateReport(command.reasons(), command.comment());
        } catch (IllegalArgumentException error) {
            throw new QuestionReportProblems.InvalidQuestionReportProblem(
                    command.threadId(), error.getMessage());
        }

        String reportId = UUID.randomUUID().toString();
        AggregateLifecycle.apply(new QuestionReportEvent.QuestionReportedEvent(
                command.threadId(),
                reportId,
                command.questionId(),
                command.topicId(),
                command.topicCreatorId(),
                command.reporterId(),
                command.gameId(),
                command.reasons(),
                command.comment(),
                command.language(),
                command.snapshot(),
                command.reportedAt()));
        return new QuestionReportResult(reportId, true);
    }

    @CommandHandler
    public void handle(QuestionReportCommand.ResolveQuestionReportCommand command) {
        QuestionReportStatus status = reports.get(command.reportId());
        if (status == null) {
            throw new QuestionReportProblems.QuestionReportNotFoundProblem(command.reportId());
        }
        if (status != QuestionReportStatus.OPEN) {
            return;
        }
        try {
            QuestionReportRules.validateNote(command.note());
        } catch (IllegalArgumentException error) {
            throw new QuestionReportProblems.InvalidQuestionReportProblem(
                    command.reportId(), error.getMessage());
        }

        AggregateLifecycle.apply(new QuestionReportEvent.QuestionReportReviewedEvent(
                command.threadId(),
                command.reportId(),
                command.resolution().status(),
                command.reviewerId(),
                command.note(),
                command.reviewedAt()));
    }

    @EventSourcingHandler
    public void on(QuestionReportEvent.QuestionReportedEvent event) {
        this.threadId = event.threadId();
        this.questionId = event.questionId();
        this.reporterId = event.reporterId();
        this.reports.put(event.reportId(), QuestionReportStatus.OPEN);
        this.currentOpenReportId = event.reportId();
    }

    @EventSourcingHandler
    public void on(QuestionReportEvent.QuestionReportReviewedEvent event) {
        this.reports.put(event.reportId(), event.status());
        if (event.reportId().equals(this.currentOpenReportId)) {
            this.currentOpenReportId = null;
        }
    }
}
