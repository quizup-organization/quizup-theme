package io.github.quizup.theme.infrastructure.config.seed;

import io.github.quizup.theme.domain.model.TopicCategory;

import java.util.List;

/**
 * Définition validée d'un thème issu d'un fichier de seed YAML.
 */
public record TopicSeedDefinition(
        String topicId,
        String name,
        String description,
        TopicCategory category,
        String imageUrl,
        List<QuestionSeedDefinition> questions) {
}
