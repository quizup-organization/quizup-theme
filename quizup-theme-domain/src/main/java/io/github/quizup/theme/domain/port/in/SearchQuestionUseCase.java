package io.github.quizup.theme.domain.port.in;

import io.github.quizup.microservice.core.infrastructure.in.api.request.SearchRequest;
import io.github.quizup.microservice.core.infrastructure.in.api.response.SearchResponse;
import io.github.quizup.theme.domain.model.Question;
import io.github.quizup.theme.domain.query.QuestionQuery;

import java.util.concurrent.CompletableFuture;

public interface SearchQuestionUseCase {

    CompletableFuture<SearchResponse<Question>> search(QuestionQuery.QuestionSearchQuery query);

    default CompletableFuture<SearchResponse<Question>> search(SearchRequest request) {
        return search(new QuestionQuery.QuestionSearchQuery(request));
    }
}
