package io.github.quizup.theme.domain.query;

import io.github.quizup.microservice.core.domain.model.i18n.Language;
import io.github.quizup.microservice.core.infrastructure.in.api.request.SearchRequest;

import java.util.Set;

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
     * Query pour récupérer toutes les questions d'un thème (seed, réparation)
     */
    record GetQuestionsByTopicIdQuery(
            String topicId
    ) {
    }

    /**
     * Query pour récupérer des questions aléatoires approuvées pour un duel, disponibles dans
     * **toutes** les langues demandées (sélection stricte).
     */
    record GetRandomApprovedQuestionsQuery(
            String topicId,
            int count,
            Set<Language> languages
    ) {
    }

    /**
     * Query de comptage des questions approuvées disponibles dans toutes les langues demandées
     * (garde matchmaking/défi).
     */
    record CountApprovedQuestionsByTopicAndLanguagesQuery(
            String topicId,
            Set<Language> languages
    ) {
    }
}
