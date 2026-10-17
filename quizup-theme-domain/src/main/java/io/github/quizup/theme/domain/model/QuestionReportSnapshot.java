package io.github.quizup.theme.domain.model;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;

import io.github.quizup.microservice.core.domain.model.i18n.Language;

/**
 * Contenu de la question tel que le joueur l'a vu au moment du signalement.
 *
 * <p>Les réponses sont indexées par la lettre du choix ({@code A}–{@code D}) ; les traductions
 * reprennent les contenus localisés de la partie. {@code language} est la langue d'affichage du
 * signalant, utilisée pour le rendu par défaut côté modération.</p>
 */
public record QuestionReportSnapshot(
        String questionText,
        String imageUrl,
        Map<String, String> answers,
        Map<String, QuestionReportTranslation> translations,
        String correctAnswer,
        String language,
        QuestionReportSnapshotSource source
) {

    public QuestionReportSnapshot {
        answers = answers == null ? Map.of() : Map.copyOf(answers);
        translations = translations == null ? Map.of() : Map.copyOf(translations);
    }

    /** Repli : contenu courant de la question, dans la langue du signalant (repli FR). */
    public static QuestionReportSnapshot current(Question question, Language language) {
        if (question == null) {
            return null;
        }
        Map<String, QuestionReportTranslation> translations = new LinkedHashMap<>();
        for (Map.Entry<Language, QuestionContent> entry : question.contents().entrySet()) {
            translations.put(entry.getKey().code(), new QuestionReportTranslation(
                    entry.getValue().text(),
                    toAnswerMap(entry.getValue().answers())));
        }
        QuestionContent content = question.content(language == null ? Language.FR : language);
        return new QuestionReportSnapshot(
                content.text(),
                question.imageUrl(),
                toAnswerMap(content.answers()),
                translations,
                question.correctAnswer() == null ? null : question.correctAnswer().name(),
                language == null ? Language.FR.code() : language.code(),
                QuestionReportSnapshotSource.CURRENT);
    }

    public static Map<String, String> toAnswerMap(Map<QuestionChoice, String> answers) {
        Map<String, String> result = new LinkedHashMap<>();
        if (answers == null) {
            return result;
        }
        Map<QuestionChoice, String> sorted = new EnumMap<>(QuestionChoice.class);
        sorted.putAll(answers);
        for (Map.Entry<QuestionChoice, String> entry : sorted.entrySet()) {
            result.put(entry.getKey().name(), entry.getValue());
        }
        return result;
    }
}
