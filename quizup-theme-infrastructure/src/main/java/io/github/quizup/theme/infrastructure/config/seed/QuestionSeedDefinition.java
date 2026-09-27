package io.github.quizup.theme.infrastructure.config.seed;

import io.github.quizup.theme.domain.model.QuestionChoice;

import java.util.Map;

/**
 * Définition validée d'une question issue d'un fichier de seed YAML.
 */
public record QuestionSeedDefinition(
        String text,
        Map<QuestionChoice, String> answers,
        QuestionChoice correctAnswer,
        String imageUrl) {
}
