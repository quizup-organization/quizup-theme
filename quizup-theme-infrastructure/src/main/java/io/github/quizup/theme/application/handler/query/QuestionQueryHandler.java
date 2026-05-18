package io.github.quizup.theme.application.handler.query;

import io.github.quizup.common.domain.model.search.PageResult;
import io.github.quizup.theme.domain.exception.QuestionProblems;
import io.github.quizup.theme.domain.model.Question;
import io.github.quizup.theme.domain.port.out.QuestionRepositoryPort;
import io.github.quizup.theme.domain.query.QuestionQuery;
import org.axonframework.queryhandling.QueryHandler;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class QuestionQueryHandler {

    private final QuestionRepositoryPort questionRepositoryPort;

    public QuestionQueryHandler(QuestionRepositoryPort questionRepositoryPort) {
        this.questionRepositoryPort = questionRepositoryPort;
    }

    @QueryHandler
    public Question handle(QuestionQuery.GetQuestionByIdQuery query) {
        return questionRepositoryPort.findById(query.questionId())
                .orElseThrow(() -> new QuestionProblems.QuestionNotFoundProblem(query.questionId()));
    }

    @QueryHandler
    public List<Question> handle(QuestionQuery.GetRandomApprovedQuestionsQuery query) {
        return questionRepositoryPort.findRandomApprovedByTopicId(query.topicId(), query.count());
    }

    @QueryHandler
    public Integer handle(QuestionQuery.CountApprovedQuestionsByTopicQuery query) {
        return questionRepositoryPort.countApprovedByTopicId(query.topicId());
    }

    @QueryHandler
    public PageResult<Question> handle(QuestionQuery.QuestionSearchQuery query) {
        return questionRepositoryPort.findAll(query);
    }
}
