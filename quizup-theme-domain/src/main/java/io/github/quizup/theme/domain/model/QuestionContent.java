package io.github.quizup.theme.domain.model;

import io.github.quizup.microservice.core.domain.model.i18n.Language;

import java.util.Map;

/**
 * Contenu d'une question dans une langue : texte + réponses A-D.
 * Le choix correct ({@link Question#correctAnswer()}) est porté par la question et partagé
 * entre les langues.
 */
public record QuestionContent(
        Language language,
        String text,
        Map<QuestionChoice, String> answers
) {
}
