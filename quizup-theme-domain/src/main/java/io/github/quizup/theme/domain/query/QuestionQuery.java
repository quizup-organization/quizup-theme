package io.github.quizup.theme.domain.query;

import io.github.quizup.common.domain.model.search.FilterCriteria;
import io.github.quizup.common.domain.model.search.PageCriteria;
import io.github.quizup.common.domain.model.search.SortCriteria;
import io.github.quizup.common.domain.query.SearchQuery;

import java.util.List;

public interface QuestionQuery {

    record QuestionSearchQuery(
            List<FilterCriteria> filters,
            List<SortCriteria> sorts,
            PageCriteria page
    ) implements QuestionQuery, SearchQuery {
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

    record CountApprovedQuestionsByTopicQuery(
            String topicId
    ) {
    }
}
