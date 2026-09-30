package io.github.quizup.theme.infrastructure.config.seed;

import io.github.quizup.microservice.core.domain.model.i18n.Language;
import io.github.quizup.theme.domain.model.Question;

/**
 * Identité stable d'une question de seed : le texte peut être dupliqué dans un thème
 * (questions visuelles, ex. « Quel est ce pays ? »), la paire (texte, imageUrl) est en
 * revanche unique par fichier. Sert de clé de réparation à {@code DataSeeder}.
 * <p>Le texte de référence est le contenu français (toujours présent dans le seed).</p>
 */
public record QuestionIdentity(String text, String imageUrl) {

    public static QuestionIdentity of(QuestionSeedDefinition definition) {
        return new QuestionIdentity(definition.contents().get(Language.FR).text(), definition.imageUrl());
    }

    public static QuestionIdentity of(Question question) {
        return new QuestionIdentity(question.text(), question.imageUrl());
    }
}
