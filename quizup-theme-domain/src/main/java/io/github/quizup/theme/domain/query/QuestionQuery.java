package io.github.quizup.theme.domain.query;

import io.github.quizup.microservice.core.domain.model.i18n.Language;
import io.github.quizup.microservice.core.infrastructure.in.api.request.SearchRequest;
import io.github.quizup.theme.domain.model.QuestionDifficulty;

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
     * <p>
     * {@code difficulties} et {@code excludeQuestionIds} sont des <b>préférences de tri</b>, pas
     * des filtres (best-effort) : les questions jamais jouées passent avant les déjà-jouées, la
     * difficulté cible avant les difficultés inconnues, elles-mêmes avant les autres — aléatoire
     * à chaque rang. Le résultat peut donc contenir des questions déjà jouées ou d'une autre
     * difficulté si le thème n'en fournit pas assez.
     */
    record GetRandomApprovedQuestionsQuery(
            String topicId,
            int count,
            Set<Language> languages,
            Set<QuestionDifficulty> difficulties,
            Set<String> excludeQuestionIds
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
