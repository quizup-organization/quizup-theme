package io.github.quizup.theme.domain.model;

import io.github.quizup.microservice.core.domain.model.i18n.Language;
import lombok.Builder;

import java.time.Instant;
import java.util.Set;

/**
 * Signalement d'une question par un joueur.
 *
 * <p>Un <b>thread</b> regroupe les signalements successifs d'un même joueur sur une même
 * question ({@code threadId} déterministe). Un seul signalement est {@code OPEN} à la fois ;
 * l'historique des signalements clos est conservé.</p>
 */
@Builder(toBuilder = true)
public record QuestionReport(
        String reportId,
        String threadId,
        String questionId,
        String topicId,
        String reporterId,
        String gameId,
        Set<QuestionReportReason> reasons,
        String comment,
        Language language,
        QuestionReportSnapshot snapshot,
        QuestionReportStatus status,
        Instant createdAt,
        String reviewedBy,
        Instant reviewedAt,
        String resolutionNote
) {

    public QuestionReport {
        reasons = reasons == null ? Set.of() : Set.copyOf(reasons);
    }

    public boolean open() {
        return status == QuestionReportStatus.OPEN;
    }
}
