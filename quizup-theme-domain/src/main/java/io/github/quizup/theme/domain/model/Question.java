package io.github.quizup.theme.domain.model;

import io.github.quizup.microservice.core.domain.model.i18n.Language;
import lombok.Builder;

import java.time.Instant;
import java.util.Map;

/**
 * Modèle domaine d'une question multilingue : un contenu source
 * ({@link #sourceLanguage()}) et ses traductions ({@link #translations()}, source incluse).
 */
@Builder(toBuilder = true)
public record Question(
        String questionId,
        String topicId,
        Language sourceLanguage,
        Map<Language, QuestionContent> translations,
        String imageUrl,
        QuestionChoice correctAnswer,
        QuestionStatus status,
        QuestionDifficulty difficulty,
        String creatorId,
        String updatedBy,
        Instant createdAt,
        Instant updatedAt
) {

    /**
     * Contenu dans la langue demandée, avec repli sur la langue source si la traduction
     * n'existe pas encore.
     */
    public QuestionContent content(Language language) {
        QuestionContent content = translations.get(language);
        return content != null ? content : translations.get(sourceLanguage);
    }

    /** Texte dans la langue source (compatibilité lecture). */
    public String text() {
        return content(sourceLanguage).text();
    }

    /** Réponses dans la langue source (compatibilité lecture). */
    public Map<QuestionChoice, String> answers() {
        return content(sourceLanguage).answers();
    }
}
