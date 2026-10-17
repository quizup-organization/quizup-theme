package io.github.quizup.theme.domain.model;

import io.github.quizup.microservice.core.domain.model.i18n.Language;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Règles pures des signalements de questions : identifiants déterministes, validation des
 * entrées et politique de suspension automatique.
 */
public interface QuestionReportRules {

    int MAX_COMMENT_LENGTH = 500;
    int MAX_NOTE_LENGTH = 500;

    /** Identifiant déterministe du thread (joueur × question) : sérialise les signalements. */
    static String threadId(String questionId, String reporterId) {
        String seed = Objects.requireNonNull(questionId, "questionId") + "|"
                + Objects.requireNonNull(reporterId, "reporterId");
        return UUID.nameUUIDFromBytes(seed.getBytes(StandardCharsets.UTF_8)).toString();
    }

    static void validateReport(Set<QuestionReportReason> reasons, String comment) {
        if (reasons == null || reasons.isEmpty()) {
            throw new IllegalArgumentException("At least one report reason is required");
        }
        if (comment != null && comment.length() > MAX_COMMENT_LENGTH) {
            throw new IllegalArgumentException("Report comment is too long");
        }
    }

    static void validateNote(String note) {
        if (note != null && note.length() > MAX_NOTE_LENGTH) {
            throw new IllegalArgumentException("Resolution note is too long");
        }
    }

    /**
     * Début de la fenêtre de comptage : on ne compte pas les signalements antérieurs à la
     * dernière réactivation/restauration, sinon une question réactivée serait immédiatement
     * re-suspendue par d'anciens signalements encore ouverts.
     */
    static Instant windowStart(Instant now, Duration window, Instant reinstatedAt, Instant restoredAt) {
        Instant start = now.minus(window == null ? Duration.ofDays(7) : window);
        if (reinstatedAt != null && reinstatedAt.isAfter(start)) {
            start = reinstatedAt;
        }
        if (restoredAt != null && restoredAt.isAfter(start)) {
            start = restoredAt;
        }
        return start;
    }

    static boolean shouldSuspend(int distinctReporters, int threshold) {
        return threshold > 0 && distinctReporters >= threshold;
    }

    static Language languageOrNull(String language) {
        return language == null || language.isBlank() ? null : Language.fromCode(language);
    }
}
