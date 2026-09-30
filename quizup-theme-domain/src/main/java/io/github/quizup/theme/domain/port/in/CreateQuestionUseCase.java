package io.github.quizup.theme.domain.port.in;

import io.github.quizup.microservice.core.domain.model.i18n.Language;
import io.github.quizup.theme.domain.command.QuestionCommand;
import io.github.quizup.theme.domain.model.QuestionChoice;
import io.github.quizup.theme.domain.model.QuestionContent;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

public interface CreateQuestionUseCase {

    CompletableFuture<String> create(QuestionCommand.CreateQuestionCommand command);

    default CompletableFuture<String> create(String questionId,
                                           String topicId,
                                           Map<Language, QuestionContent> contents,
                                           QuestionChoice correctAnswer,
                                           String imageUrl,
                                           String creatorId) {
        return create(
                new QuestionCommand.CreateQuestionCommand(
                        questionId,
                        topicId,
                        contents,
                        correctAnswer,
                        imageUrl,
                        creatorId
                )
        );
    }

    default void createAndWait(String questionId,
                               String topicId,
                               Map<Language, QuestionContent> contents,
                               QuestionChoice correctAnswer,
                               String imageUrl,
                               String creatorId) {
        create(questionId, topicId, contents, correctAnswer, imageUrl, creatorId).join();
    }
}
