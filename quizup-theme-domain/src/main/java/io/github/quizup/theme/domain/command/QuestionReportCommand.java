package io.github.quizup.theme.domain.command;

import io.github.quizup.microservice.core.domain.model.i18n.Language;
import io.github.quizup.theme.domain.model.QuestionReportReason;
import io.github.quizup.theme.domain.model.QuestionReportResolution;
import io.github.quizup.theme.domain.model.QuestionReportSnapshot;
import org.axonframework.modelling.command.TargetAggregateIdentifier;

import java.time.Instant;
import java.util.Set;

public interface QuestionReportCommand {

    String threadId();

    /**
     * Dépose un signalement. Le handler est idempotent : si un signalement est déjà ouvert sur
     * le thread (joueur × question), aucun événement n'est émis et l'identifiant existant est
     * retourné.
     */
    record ReportQuestionCommand(
            @TargetAggregateIdentifier String threadId,
            String questionId,
            String topicId,
            String topicCreatorId,
            String reporterId,
            String gameId,
            Set<QuestionReportReason> reasons,
            String comment,
            Language language,
            QuestionReportSnapshot snapshot,
            Instant reportedAt
    ) implements QuestionReportCommand {
    }

    /** Clôture un signalement (résolu ou ignoré). Idempotent sur un signalement déjà clos. */
    record ResolveQuestionReportCommand(
            @TargetAggregateIdentifier String threadId,
            String reportId,
            QuestionReportResolution resolution,
            String reviewerId,
            String note,
            Instant reviewedAt
    ) implements QuestionReportCommand {
    }
}
