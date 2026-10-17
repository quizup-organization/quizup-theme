package io.github.quizup.theme.domain.query;

import io.github.quizup.microservice.core.domain.model.i18n.Language;
import io.github.quizup.microservice.core.infrastructure.in.api.request.SearchRequest;
import io.github.quizup.theme.domain.model.QuestionAdminFilter;
import io.github.quizup.theme.domain.model.QuestionAdminSort;
import io.github.quizup.theme.domain.model.QuestionDifficulty;

import java.util.List;
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

    /** Récupération batch ; l'ordre demandé est préservé, les ids inconnus sont ignorés. */
    record GetQuestionsByIdsQuery(
            List<String> questionIds
    ) {
    }

    /** Catalogue admin filtré (tous statuts, flags de modération, texte). */
    record GetQuestionAdminPageQuery(
            QuestionAdminFilter filter,
            QuestionAdminSort sort,
            int page,
            int size
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

    /**
     * Couverture linguistique d'un thème : pour chaque combinaison non vide des langues supportées,
     * le nombre de questions approuvées présentes dans <b>toutes</b> les langues de la combinaison.
     * Préparée par la façade (création d'un ticket d'appariement) et portée par l'événement de
     * démarrage — la saga d'appariement compare localement, sans query bloquante.
     */
    record GetTopicLanguageCoverageQuery(
            String topicId
    ) {
    }
}
