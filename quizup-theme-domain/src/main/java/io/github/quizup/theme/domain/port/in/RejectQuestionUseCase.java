package io.github.quizup.theme.domain.port.in;

import io.github.quizup.theme.domain.command.QuestionCommand;

import java.util.concurrent.CompletableFuture;

public interface RejectQuestionUseCase {
    CompletableFuture<String> reject(QuestionCommand.RejectQuestionCommand command);

    default CompletableFuture<String> reject(String questionId, String requesterId, String reason) {
        return reject(
                new QuestionCommand.RejectQuestionCommand(
                        questionId,
                        requesterId,
                        reason
                )
        );
    }
}

