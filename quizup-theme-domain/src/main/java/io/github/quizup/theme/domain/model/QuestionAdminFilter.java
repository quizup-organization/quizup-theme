package io.github.quizup.theme.domain.model;

/**
 * Filtre du catalogue admin. Tous les champs sont facultatifs ({@code null} = pas de filtre) ;
 * {@code moderation} vaut {@link QuestionModerationFilter#ALL} par défaut.
 */
public record QuestionAdminFilter(
        String text,
        String topicId,
        QuestionStatus status,
        QuestionDifficulty difficulty,
        QuestionModerationFilter moderation,
        Boolean reportedOnly
) {
}
