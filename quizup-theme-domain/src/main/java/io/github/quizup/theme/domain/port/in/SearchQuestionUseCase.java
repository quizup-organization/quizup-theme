package io.github.quizup.theme.domain.port.in;

import io.github.quizup.common.domain.model.search.*;
import io.github.quizup.theme.domain.model.Question;
import io.github.quizup.theme.domain.query.QuestionQuery;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface SearchQuestionUseCase {

    CompletableFuture<PageResult<Question>> search(QuestionQuery.QuestionSearchQuery query);

    default CompletableFuture<PageResult<Question>> search(List<FilterCriteria> filters,
                                                           List<SortCriteria> sorts,
                                                           PageCriteria page) {
        return search(
                new QuestionQuery.QuestionSearchQuery(
                        filters,
                        sorts,
                        page
                )
        );

    }
}

