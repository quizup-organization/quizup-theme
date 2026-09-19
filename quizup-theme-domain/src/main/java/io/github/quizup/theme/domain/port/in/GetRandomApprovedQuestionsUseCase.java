package io.github.quizup.theme.domain.port.in;

import io.github.quizup.theme.domain.model.Question;
import io.github.quizup.theme.domain.query.QuestionQuery;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface GetRandomApprovedQuestionsUseCase {

    CompletableFuture<List<Question>> getRandomApprovedQuestions(QuestionQuery.GetRandomApprovedQuestionsQuery query);

    default CompletableFuture<List<Question>> getRandomApprovedQuestions(String topicId, int count) {
        return getRandomApprovedQuestions(
                new QuestionQuery.GetRandomApprovedQuestionsQuery(
                        topicId,
                        count
                )
        );
    }
}

