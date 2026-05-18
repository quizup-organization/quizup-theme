package io.github.quizup.theme.infrastructure.in.api.request;

import io.github.quizup.theme.domain.model.TopicCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public interface TopicRequest {

    /**
     * Requête de création d'un thème
     */
    record CreateTopicRequest(
            @NotBlank(message = "Le nom du thème est obligatoire")
            @Size(max = 25, message = "Le nom du thème ne peut pas dépasser 25 caractères")
            String name,

            @Size(max = 500, message = "La description ne peut pas dépasser 500 caractères")
            String description,

            @NotNull(message = "La catégorie est obligatoire")
            TopicCategory category
    ) {
    }
}
