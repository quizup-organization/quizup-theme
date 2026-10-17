package io.github.quizup.theme.domain.model;

/** Origine d'une suspension de question. */
public enum QuestionSuspensionReason {
    /** Seuil automatique de signalements distincts atteint. */
    AUTO_REPORT_THRESHOLD,
    /** Suspension manuelle par un administrateur. */
    MANUAL
}
