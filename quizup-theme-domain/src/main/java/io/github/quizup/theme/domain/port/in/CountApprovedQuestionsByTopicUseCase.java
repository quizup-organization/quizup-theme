package io.github.quizup.theme.domain.port.in;

import io.github.quizup.theme.domain.query.QuestionQuery;

import java.util.concurrent.CompletableFuture;

public interface CountApprovedQuestionsByTopicUseCase {

    CompletableFuture<Integer> countApprovedQuestionsByTopic(QuestionQuery.CountApprovedQuestionsByTopicQuery query);

    default CompletableFuture<Integer> countApprovedQuestionsByTopic(String topicId){
        return countApprovedQuestionsByTopic(
                new QuestionQuery.CountApprovedQuestionsByTopicQuery(
                        topicId
                )
        );
    }
}

