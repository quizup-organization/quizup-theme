package io.github.quizup.theme.infrastructure.config.seed;

import io.github.quizup.microservice.core.domain.model.i18n.Language;
import io.github.quizup.theme.domain.model.QuestionChoice;
import io.github.quizup.theme.domain.model.QuestionContent;

import java.util.Map;

/**
 * Définition validée d'une question issue d'un fichier de seed YAML :
 * un contenu par langue disponible (le français est toujours présent, l'anglais optionnel).
 */
public record QuestionSeedDefinition(
        Map<Language, QuestionContent> contents,
        QuestionChoice correctAnswer,
        String imageUrl) {
}
