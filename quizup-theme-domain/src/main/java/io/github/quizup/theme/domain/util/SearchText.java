package io.github.quizup.theme.domain.util;

import java.text.Normalizer;

/**
 * Normalisation de texte pour la recherche : minuscules, sans accents ni diacritiques.
 * Utilisée pour alimenter/consommer le champ {@code name_normalized} (recherche insensible
 * aux accents et à la casse).
 */
public final class SearchText {

    private SearchText() {
    }

    public static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String decomposed = Normalizer.normalize(value, Normalizer.Form.NFD);
        return decomposed.replaceAll("\\p{M}+", "").toLowerCase().trim();
    }
}
