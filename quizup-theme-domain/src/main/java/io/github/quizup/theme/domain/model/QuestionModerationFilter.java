package io.github.quizup.theme.domain.model;

/** Filtre de modération du catalogue admin. */
public enum QuestionModerationFilter {
    /** Toutes les questions non archivées (actives et suspendues). */
    ALL,
    /** Approuvées, ni suspendues ni archivées. */
    ACTIVE,
    SUSPENDED,
    ARCHIVED
}
