package io.github.quizup.theme.domain.model;

import java.util.Map;

/** Contenu localisé figé dans le snapshot d'un signalement. */
public record QuestionReportTranslation(
        String text,
        Map<String, String> answers
) {
}
