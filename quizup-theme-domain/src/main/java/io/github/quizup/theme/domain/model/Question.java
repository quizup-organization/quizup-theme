package io.github.quizup.theme.domain.model;

import io.github.quizup.microservice.core.domain.model.i18n.Language;
import lombok.Builder;

import java.time.Instant;
import java.util.Map;
import java.util.Set;

/**
 * Modèle domaine d'une question multilingue : un contenu par langue disponible
 * ({@link #contents()}), sans langue source explicite. Le repli de lecture est déterministe :
 * langue demandée, puis français, puis anglais, puis premier contenu disponible.
 */
@Builder(toBuilder = true)
public record Question(
        String questionId,
        String topicId,
        Map<Language, QuestionContent> contents,
        String imageUrl,
        QuestionChoice correctAnswer,
        QuestionStatus status,
        QuestionDifficulty difficulty,
        String creatorId,
        String updatedBy,
        Instant createdAt,
        Instant updatedAt
) {

    /** Langues pour lesquelles un contenu est disponible. */
    public Set<Language> availableLanguages() {
        return Set.copyOf(contents.keySet());
    }

    public boolean hasLanguage(Language language) {
        return contents.containsKey(language);
    }

    /** Contenu dans la langue demandée, avec repli déterministe (FR, puis EN, puis premier). */
    public QuestionContent content(Language language) {
        QuestionContent content = contents.get(language);
        if (content != null) {
            return content;
        }
        content = contents.get(Language.FR);
        if (content != null) {
            return content;
        }
        content = contents.get(Language.EN);
        if (content != null) {
            return content;
        }
        return contents.values().iterator().next();
    }

    /** Texte de repli (FR prioritaire), utilisé par les vues historiques. */
    public String text() {
        return content(Language.FR).text();
    }

    /** Réponses de repli (FR prioritaire), utilisées par les vues historiques. */
    public Map<QuestionChoice, String> answers() {
        return content(Language.FR).answers();
    }
}
