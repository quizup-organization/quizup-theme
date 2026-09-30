package io.github.quizup.theme.domain.port.in;

import io.github.quizup.theme.domain.command.QuestionCommand;

import java.util.concurrent.CompletableFuture;

/**
 * Port entrant — ajout (ou remplacement) d'une traduction de question (langue source exclue).
 */
public interface AddQuestionTranslationUseCase {

    CompletableFuture<String> add(QuestionCommand.AddQuestionTranslationCommand command);
}
