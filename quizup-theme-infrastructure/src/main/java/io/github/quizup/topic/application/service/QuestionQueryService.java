package io.github.quizup.topic.application.service;

import io.github.quizup.common.domain.model.search.PageResult;
import io.github.quizup.common.infrastructure.axon.PageResponseTypes;
import io.github.quizup.topic.domain.exception.QuestionProblems;
import io.github.quizup.topic.domain.model.Question;
import io.github.quizup.topic.domain.port.in.CountApprovedQuestionsByTopicUseCase;
import io.github.quizup.topic.domain.port.in.GetRandomApprovedQuestionsUseCase;
import io.github.quizup.topic.domain.port.in.GetQuestionUseCase;
import io.github.quizup.topic.domain.port.in.SearchQuestionUseCase;
import io.github.quizup.topic.domain.query.QuestionQuery;
import org.axonframework.messaging.responsetypes.ResponseTypes;
import org.axonframework.queryhandling.QueryGateway;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
public class QuestionQueryService implements GetQuestionUseCase, SearchQuestionUseCase, GetRandomApprovedQuestionsUseCase, CountApprovedQuestionsByTopicUseCase {

    private final QueryGateway queryGateway;

    public QuestionQueryService(QueryGateway queryGateway) {
        this.queryGateway = queryGateway;
    }

    @Override
    public CompletableFuture<PageResult<Question>> search(QuestionQuery.QuestionSearchQuery query) {
        return queryGateway.query(query, PageResponseTypes.pageResultOf(Question.class));
    }
    @Override
    public CompletableFuture<Question> getById(QuestionQuery.GetQuestionByIdQuery query) throws QuestionProblems.QuestionNotFoundProblem {
        return queryGateway.query(query, ResponseTypes.instanceOf(Question.class));
    }

    @Override
    public CompletableFuture<List<Question>> getRandomApprovedQuestions(QuestionQuery.GetRandomApprovedQuestionsQuery query) {
        return queryGateway.query(query, ResponseTypes.multipleInstancesOf(Question.class));
    }

    @Override
    public CompletableFuture<Integer> countApprovedQuestionsByTopic(QuestionQuery.CountApprovedQuestionsByTopicQuery query) {
        return queryGateway.query(query, ResponseTypes.instanceOf(Integer.class));
    }
}

