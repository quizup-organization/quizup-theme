package io.github.quizup.theme.application.service;

import io.github.quizup.theme.domain.command.QuestionCommand;
import io.github.quizup.theme.domain.port.in.ApproveQuestionUseCase;
import io.github.quizup.theme.domain.port.in.CreateQuestionUseCase;
import io.github.quizup.theme.domain.port.in.RejectQuestionUseCase;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
public class QuestionCommandService implements CreateQuestionUseCase, ApproveQuestionUseCase, RejectQuestionUseCase {

    private final CommandGateway commandGateway;

    public QuestionCommandService(CommandGateway commandGateway) {
        this.commandGateway = commandGateway;
    }


    @Override
    public CompletableFuture<String> create(QuestionCommand.CreateQuestionCommand command) {
        return commandGateway.send(command);
    }

    @Override
    public CompletableFuture<String> approve(QuestionCommand.ApproveQuestionCommand command) {
        return commandGateway.send(command);
    }
    @Override
    public CompletableFuture<String> reject(QuestionCommand.RejectQuestionCommand command) {
        return commandGateway.send(command);
    }
}

