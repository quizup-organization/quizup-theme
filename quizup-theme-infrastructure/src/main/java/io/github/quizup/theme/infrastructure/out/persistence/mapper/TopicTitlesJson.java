package io.github.quizup.theme.infrastructure.out.persistence.mapper;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.quizup.microservice.core.domain.model.i18n.Language;

import java.util.Map;

/**
 * (Dé)sérialisation JSON des titres personnalisés d'un sujet, stockés dans une colonne
 * {@code TEXT} ({@code topic_entry.title_names}) : {@code {"FR":{"CONFIRME":"Historien"}}}.
 */
public final class TopicTitlesJson {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final TypeReference<Map<Language, Map<String, String>>> TYPE =
            new TypeReference<>() {
            };

    private TopicTitlesJson() {
    }

    /** Sérialise la carte des titres, ou {@code null} si rien n'est défini. */
    public static String write(Map<Language, Map<String, String>> titles) {
        if (titles == null || titles.isEmpty()) {
            return null;
        }
        try {
            return MAPPER.writeValueAsString(titles);
        } catch (Exception error) {
            throw new IllegalStateException("Failed to serialize topic titles", error);
        }
    }

    /** Relit la carte des titres depuis la colonne, ou {@code null} si vide/illisible. */
    public static Map<Language, Map<String, String>> read(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return MAPPER.readValue(json, TYPE);
        } catch (Exception error) {
            return null;
        }
    }
}
