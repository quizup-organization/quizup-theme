package io.github.quizup.theme.infrastructure.in.api.request;

import io.github.quizup.theme.domain.model.QuestionChoice;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Map;

public interface QuestionRequest {
    /**
     * Requête de rejet d'une question
     */
    record RejectQuestionRequest(
            @NotBlank(message = "La raison du rejet est obligatoire")
            String reason
    ) {
    }

    /**
     * Requête d'ajout d'une question à un thème
     */
    record CreateQuestionRequest(
            @NotBlank(message = "Le topic relatif à la question est obligatoire")
            String topicId,

            @NotBlank(message = "Le texte de la question est obligatoire")
            @Size(max = 135, message = "La question ne peut pas dépasser 135 caractères")
            String text,

            @NotNull(message = "Les réponses sont obligatoires")
            Map<QuestionChoice, @Size(max = 30, message = "Une réponse ne peut pas dépasser 30 caractères") String> answers,

            @NotNull(message = "La bonne réponse est obligatoire")
            QuestionChoice correctAnswer
    ) {
    }
}
