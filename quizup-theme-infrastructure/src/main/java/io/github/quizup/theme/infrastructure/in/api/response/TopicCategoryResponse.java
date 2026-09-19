package io.github.quizup.theme.infrastructure.in.api.response;

import io.github.quizup.theme.domain.model.TopicCategory;

import java.io.Serializable;

/**
 * Catégorie de thème exposée au client (code + libellé FR).
 */
public record TopicCategoryResponse(
        TopicCategory category,
        String label
) implements Serializable {
}
