package io.github.quizup.theme.infrastructure.config.seed;

import io.github.quizup.microservice.core.domain.model.i18n.Language;
import io.github.quizup.theme.domain.model.QuestionChoice;
import io.github.quizup.theme.domain.model.QuestionContent;

import java.util.Map;

/**
 * Définition validée d'une question issue d'un fichier de seed YAML :
 * contenu source (+ sa langue) et traductions.
 */
public record QuestionSeedDefinition(
        Language sourceLanguage,
        String text,
        Map<QuestionChoice, String> answers,
        QuestionChoice correctAnswer,
        String imageUrl,
        Map<Language, QuestionContent> translations) {
}
