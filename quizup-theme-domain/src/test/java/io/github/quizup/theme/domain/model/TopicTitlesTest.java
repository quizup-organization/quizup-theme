package io.github.quizup.theme.domain.model;

import io.github.quizup.microservice.core.domain.model.i18n.Language;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Règles des titres personnalisés (grade connu, libellé non vide et borné). */
class TopicTitlesTest {

    @Test
    void normalizesLabelsAndDropsEmptyLanguages() {
        Map<Language, Map<String, String>> raw = new LinkedHashMap<>();
        raw.put(Language.FR, Map.of("CONFIRME", "  Historien confirmé  "));
        raw.put(Language.EN, Map.of());

        Map<Language, Map<String, String>> titles = TopicTitles.normalize(raw);

        assertNotNull(titles);
        assertEquals(Set.of(Language.FR), titles.keySet());
        assertEquals("Historien confirmé", titles.get(Language.FR).get("CONFIRME"));
    }

    @Test
    void returnsNullWhenNothingIsDefined() {
        assertNull(TopicTitles.normalize(null));
        assertNull(TopicTitles.normalize(Map.of()));
    }

    @Test
    void rejectsUnknownGrade() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> TopicTitles.normalize(Map.of(Language.FR, Map.of("GURU", "Gourou"))));
        assertTrue(error.getMessage().contains("GURU"));
    }

    @Test
    void rejectsBlankLabel() {
        assertThrows(IllegalArgumentException.class,
                () -> TopicTitles.normalize(Map.of(Language.FR, Map.of("EXPERT", "  "))));
    }

    @Test
    void rejectsTooLongLabel() {
        String label = "x".repeat(TopicTitles.MAX_LABEL_LENGTH + 1);
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> TopicTitles.normalize(Map.of(Language.FR, Map.of("EXPERT", label))));
        assertTrue(error.getMessage().contains("too long"));
    }
}
