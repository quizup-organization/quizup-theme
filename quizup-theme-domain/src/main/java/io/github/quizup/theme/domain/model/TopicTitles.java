package io.github.quizup.theme.domain.model;

import io.github.quizup.microservice.core.domain.model.i18n.Language;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Titres **thématiques personnalisés** d'un sujet : un libellé par **grade de maîtrise** et par
 * langue (ex. Histoire → {@code CONFIRME: "Historien confirmé"}). Optionnels (le client retombe
 * alors sur « grade · nom du sujet »).
 *
 * <p>Les clés de grade sont le **miroir** de `MasteryGrade` (quizup-profile) — le domaine theme ne
 * dépend jamais d'un autre `*-domain` — et sont validées ici : toute clé inconnue est rejetée.</p>
 */
public final class TopicTitles {

    /** Clés de grade attendues (miroir de `MasteryGrade`), dans l'ordre croissant. */
    public static final List<String> GRADES = List.of(
            "NOVICE", "APPRENTI", "PRATIQUANT", "CONFIRME", "EXPERT", "MAITRE", "LEGENDE");

    public static final int MAX_LABEL_LENGTH = 40;

    private TopicTitles() {
    }

    /**
     * Normalise et valide la carte des titres : langues et clés de grade inconnues rejetées,
     * libellés vides ou trop longs rejetés, entrées vides supprimées. Renvoie {@code null} si
     * rien n'est défini (titre absent = repli client).
     */
    public static Map<Language, Map<String, String>> normalize(
            Map<Language, Map<String, String>> titles) {
        if (titles == null || titles.isEmpty()) {
            return null;
        }
        Map<Language, Map<String, String>> clean = new LinkedHashMap<>();
        titles.forEach((language, byGrade) -> {
            if (language == null) {
                throw new IllegalArgumentException("title language is required");
            }
            if (byGrade == null || byGrade.isEmpty()) {
                return;
            }
            Map<String, String> labels = new LinkedHashMap<>();
            byGrade.forEach((grade, label) -> {
                String key = requireGrade(grade);
                if (label == null || label.isBlank()) {
                    throw new IllegalArgumentException("title label is required for " + key);
                }
                String value = label.trim();
                if (value.length() > MAX_LABEL_LENGTH) {
                    throw new IllegalArgumentException(
                            "title label too long for " + key + " (" + value.length() + " > "
                                    + MAX_LABEL_LENGTH + ")");
                }
                labels.put(key, value);
            });
            if (!labels.isEmpty()) {
                clean.put(language, labels);
            }
        });
        return clean.isEmpty() ? null : clean;
    }

    private static String requireGrade(String grade) {
        String key = grade == null ? "" : grade.trim().toUpperCase();
        if (!GRADES.contains(key)) {
            throw new IllegalArgumentException("unknown mastery grade '" + grade + "'");
        }
        return key;
    }
}
