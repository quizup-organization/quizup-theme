package io.github.quizup.theme.domain.port.in;

import io.github.quizup.theme.domain.exception.QuestionProblems;
import io.github.quizup.theme.domain.model.Question;
import io.github.quizup.theme.domain.query.QuestionQuery;

import java.util.concurrent.CompletableFuture;

public interface GetQuestionUseCase {

    CompletableFuture<Question> getById(QuestionQuery.GetQuestionByIdQuery query) throws QuestionProblems.QuestionNotFoundProblem;

    default CompletableFuture<Question> getById(String questionId) throws QuestionProblems.QuestionNotFoundProblem {
        return getById(
                new QuestionQuery.GetQuestionByIdQuery(
                        questionId
                )
        );
    }
}

