package io.github.quizup.theme.domain.port.in;

import io.github.quizup.theme.domain.command.QuestionCommand;
import io.github.quizup.theme.domain.model.QuestionDifficulty;

import java.util.concurrent.CompletableFuture;

/**
 * Applique la difficulté initiale d'une question issue du seed. La classification reste
 * évolutive : le recalcul sur les statistiques de réponses reprend la main dès que
 * l'échantillon est suffisant (le seed ne remplace jamais une difficulté déjà calculée).
 */
public interface UpdateQuestionDifficultyUseCase {

    CompletableFuture<String> updateDifficulty(QuestionCommand.UpdateQuestionDifficultyCommand command);

    default void updateDifficultyAndWait(String questionId, QuestionDifficulty difficulty) {
        updateDifficulty(new QuestionCommand.UpdateQuestionDifficultyCommand(questionId, difficulty)).join();
    }
}
