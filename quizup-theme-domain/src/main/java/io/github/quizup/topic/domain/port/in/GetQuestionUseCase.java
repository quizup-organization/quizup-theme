package io.github.quizup.topic.domain.port.in;

import io.github.quizup.topic.domain.exception.QuestionProblems;
import io.github.quizup.topic.domain.model.Question;
import io.github.quizup.topic.domain.query.QuestionQuery;

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

