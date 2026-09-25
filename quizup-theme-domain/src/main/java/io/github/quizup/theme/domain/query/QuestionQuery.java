package io.github.quizup.theme.domain.query;

import io.github.quizup.microservice.core.infrastructure.in.api.request.SearchRequest;

public interface QuestionQuery {

    record QuestionSearchQuery(SearchRequest request) implements QuestionQuery {
    }

    /**
     * Query pour récupérer une question par son ID
     */
    record GetQuestionByIdQuery(
            String questionId
    ) {
    }

    /**
     * Query pour récupérer des questions aléatoires approuvées pour un duel
     */
    record GetRandomApprovedQuestionsQuery(
            String topicId,
            int count
    ) {
    }
}
