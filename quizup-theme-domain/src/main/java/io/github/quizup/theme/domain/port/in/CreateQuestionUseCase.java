package io.github.quizup.theme.domain.port.in;

import io.github.quizup.theme.domain.command.QuestionCommand;
import io.github.quizup.theme.domain.model.QuestionChoice;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

public interface CreateQuestionUseCase {

    CompletableFuture<String> create(QuestionCommand.CreateQuestionCommand command);

    default CompletableFuture<String> create(String questionId,
                                           String topicId,
                                           String text,
                                           Map<QuestionChoice, String> answers,
                                           QuestionChoice correctAnswer,
                                           String creatorId) {

        return create(
                new QuestionCommand.CreateQuestionCommand(
                        questionId,
                        topicId,
                        text,
                        answers,
                        correctAnswer,
                        creatorId
                )
        );
    }

    default void createAndWait(String questionId,
                               String topicId,
                               String text,
                               Map<QuestionChoice, String> answers,
                               QuestionChoice correctAnswer,
                               String creatorId) {
        create(questionId, topicId, text, answers, correctAnswer, creatorId).join();
    }
}

