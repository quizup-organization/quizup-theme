package io.github.quizup.theme.domain.model;

/** Motif d'un signalement de question déposé par un joueur. */
public enum QuestionReportReason {
    /** Énoncé ambigu, faute, question incompréhensible. */
    QUESTION,
    /** Image absente, erronée ou de mauvaise qualité. */
    IMAGE,
    /** Bonne réponse incorrecte ou réponses incohérentes. */
    ANSWERS
}
