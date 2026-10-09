package io.github.quizup.theme.infrastructure.config.seed;

import io.github.quizup.microservice.core.domain.model.i18n.Language;
import io.github.quizup.theme.domain.model.TopicCategory;

import java.util.List;
import java.util.Map;

/**
 * Définition validée d'un thème issu d'un fichier de seed YAML.
 */
public record TopicSeedDefinition(
        String topicId,
        Map<Language, String> names,
        String description,
        TopicCategory category,
        String imageUrl,
        List<QuestionSeedDefinition> questions) {

    /** Nom d'affichage/robuste pour les logs (FR prioritaire). */
    public String displayName() {
        return names.getOrDefault(Language.FR, names.values().stream().findFirst().orElse(topicId));
    }
}
