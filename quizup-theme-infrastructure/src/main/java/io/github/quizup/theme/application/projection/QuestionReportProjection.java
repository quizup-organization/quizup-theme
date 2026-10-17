package io.github.quizup.theme.application.projection;

import io.github.quizup.microservice.core.domain.constant.QuizUpConstants;
import io.github.quizup.theme.domain.command.QuestionCommand;
import io.github.quizup.theme.domain.event.QuestionReportEvent;
import io.github.quizup.theme.domain.model.Question;
import io.github.quizup.theme.domain.model.QuestionReport;
import io.github.quizup.theme.domain.model.QuestionReportRules;
import io.github.quizup.theme.domain.model.QuestionReportStatus;
import io.github.quizup.theme.domain.model.QuestionStatus;
import io.github.quizup.theme.domain.model.QuestionSuspensionReason;
import io.github.quizup.theme.domain.port.out.QuestionReportRepositoryPort;
import io.github.quizup.theme.domain.port.out.QuestionRepositoryPort;
import io.github.quizup.theme.infrastructure.properties.QuestionReportProperties;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.config.ProcessingGroup;
import org.axonframework.eventhandling.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Projection des signalements et politique de suspension automatique.
 *
 * <p>Après chaque nouveau signalement, le nombre de <b>signaleurs distincts</b> encore ouverts
 * dans la fenêtre configurée est compté. Le comptage ne remonte jamais avant la dernière
 * réactivation/restauration de la question : un ancien signalement ne doit pas re-suspendre une
 * question que l'administration vient de rouvrir.</p>
 */
@Component
@ProcessingGroup("theme-projection")
public class QuestionReportProjection {

    private static final Logger logger = LoggerFactory.getLogger(QuestionReportProjection.class);

    private final QuestionReportRepositoryPort reportRepository;
    private final QuestionRepositoryPort questionRepository;
    private final CommandGateway commandGateway;
    private final QuestionReportProperties properties;

    public QuestionReportProjection(QuestionReportRepositoryPort reportRepository,
                                    QuestionRepositoryPort questionRepository,
                                    CommandGateway commandGateway,
                                    QuestionReportProperties properties) {
        this.reportRepository = reportRepository;
        this.questionRepository = questionRepository;
        this.commandGateway = commandGateway;
        this.properties = properties;
    }

    @EventHandler
    @Transactional
    public void on(QuestionReportEvent.QuestionReportedEvent event) {
        reportRepository.save(QuestionReport.builder()
                .reportId(event.reportId())
                .threadId(event.threadId())
                .questionId(event.questionId())
                .topicId(event.topicId())
                .reporterId(event.reporterId())
                .gameId(event.gameId())
                .reasons(event.reasons())
                .comment(event.comment())
                .language(event.language())
                .snapshot(event.snapshot())
                .status(QuestionReportStatus.OPEN)
                .createdAt(event.reportedAt())
                .build());
        maybeSuspend(event);
    }

    @EventHandler
    @Transactional
    public void on(QuestionReportEvent.QuestionReportReviewedEvent event) {
        reportRepository.findById(event.reportId()).ifPresent(report -> reportRepository.save(
                report.toBuilder()
                        .status(event.status())
                        .reviewedBy(event.reviewerId())
                        .reviewedAt(event.reviewedAt())
                        .resolutionNote(event.note())
                        .build()));
    }

    private void maybeSuspend(QuestionReportEvent.QuestionReportedEvent event) {
        QuestionReportProperties.AutoSuspension autoSuspension = properties.autoSuspension();
        if (autoSuspension == null || !autoSuspension.enabled()) {
            return;
        }
        Question question = questionRepository.findById(event.questionId()).orElse(null);
        if (question == null
                || question.status() != QuestionStatus.APPROVED
                || question.suspended()
                || question.archived()) {
            return;
        }
        Instant since = QuestionReportRules.windowStart(
                event.reportedAt(),
                autoSuspension.window(),
                question.reinstatedAt(),
                question.restoredAt());
        int distinctReporters = reportRepository.countDistinctOpenReportersSince(event.questionId(), since);
        if (!QuestionReportRules.shouldSuspend(distinctReporters, autoSuspension.threshold())) {
            return;
        }
        long windowDays = Math.max(1, autoSuspension.window().toDays());
        String note = distinctReporters + " signalements distincts en " + windowDays + " jour(s)";
        commandGateway.send(new QuestionCommand.SuspendQuestionCommand(
                event.questionId(),
                QuestionSuspensionReason.AUTO_REPORT_THRESHOLD,
                note,
                QuizUpConstants.SYSTEM_USER_ID,
                event.reportId(),
                event.reportedAt(),
                distinctReporters,
                (int) windowDays,
                Instant.now()));
        logger.info("Question {} suspendue automatiquement : {}", event.questionId(), note);
    }
}
