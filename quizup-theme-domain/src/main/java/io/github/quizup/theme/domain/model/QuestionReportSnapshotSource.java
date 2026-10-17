package io.github.quizup.theme.domain.model;

/** Origine du contenu figé dans un signalement. */
public enum QuestionReportSnapshotSource {
    /** Version réellement jouée, extraite de l'historique de la partie. */
    PLAYED,
    /** Repli : version courante de la question au moment du signalement. */
    CURRENT
}
