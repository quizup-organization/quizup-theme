package io.github.quizup.theme.domain.port.in;

import io.github.quizup.theme.domain.command.QuestionCommand;

import java.util.concurrent.CompletableFuture;

/**
 * Port entrant — ajout (ou remplacement) de contenus localisés d'une question.
 */
public interface AddQuestionTranslationsUseCase {

    CompletableFuture<String> add(QuestionCommand.AddQuestionTranslationsCommand command);
}
