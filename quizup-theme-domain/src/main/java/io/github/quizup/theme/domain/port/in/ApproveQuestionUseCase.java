package io.github.quizup.theme.domain.port.in;

import io.github.quizup.theme.domain.command.QuestionCommand;

import java.util.concurrent.CompletableFuture;

public interface ApproveQuestionUseCase {

    CompletableFuture<String> approve(QuestionCommand.ApproveQuestionCommand command);

    default CompletableFuture<String> approve(String questionId, String requesterId) {
        return approve(
                new QuestionCommand.ApproveQuestionCommand(
                        questionId,
                        requesterId
                )
        );
    }

    default void approveAndWait(String questionId, String requesterId) {
        approve(questionId, requesterId).join();
    }
}

