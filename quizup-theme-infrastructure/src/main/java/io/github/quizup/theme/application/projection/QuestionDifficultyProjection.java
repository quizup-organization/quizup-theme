package io.github.quizup.theme.application.projection;

import io.github.quizup.game.domain.event.GameEvent;
import io.github.quizup.game.domain.model.GamePlayerType;
import io.github.quizup.microservice.core.domain.constant.QuizUpConstants;
import io.github.quizup.theme.domain.command.QuestionCommand;
import io.github.quizup.theme.domain.model.Question;
import io.github.quizup.theme.domain.model.QuestionAnswerStats;
import io.github.quizup.theme.domain.model.QuestionDifficulty;
import io.github.quizup.theme.domain.model.QuestionRules;
import io.github.quizup.theme.domain.port.out.QuestionAnswerRecordPort;
import io.github.quizup.theme.domain.port.out.QuestionAnswerStatsRepositoryPort;
import io.github.quizup.theme.domain.port.out.QuestionRepositoryPort;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.config.ProcessingGroup;
import org.axonframework.eventhandling.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * QuestionDifficultyProjection — agrège les réponses réelles d'une question puis demande à
 * l'agrégat {@code QuestionAggregate} de mettre à jour sa difficulté déduite.
 *
 * <p>Consomme {@link GameEvent.QuestionAnsweredEvent} du service {@code quizup-game}. Seules les
 * réponses <b>humaines</b> sont comptées : les réponses BOT et GHOST sont ignorées. Une
 * non-réponse (timeout) arrive en réponse fausse ({@code choice == null}, {@code correct ==
 * false}) et est donc comptée comme incorrecte. La difficulté n'est demandée qu'au-delà de
 * {@link QuestionRules#MIN_ANSWERS_FOR_DIFFICULTY} réponses.
 *
 * <p>La difficulté est portée par l'agrégat (état event-sourcé) : cette projection envoie une
 * {@link QuestionCommand.UpdateQuestionDifficultyCommand}, l'agrégat applique
 * {@code QuestionDifficultyUpdatedEvent}, et {@code QuestionProjection} met à jour le read model.
 */
@Component
@ProcessingGroup("question-difficulty-projection")
public class QuestionDifficultyProjection {

    private static final Logger logger = LoggerFactory.getLogger(QuestionDifficultyProjection.class);

    private final CommandGateway commandGateway;

    private final QuestionAnswerStatsRepositoryPort statsRepositoryPort;
    private final QuestionAnswerRecordPort answerRecordPort;
    public QuestionDifficultyProjection(CommandGateway commandGateway,
                                        QuestionAnswerStatsRepositoryPort statsRepositoryPort,
                                        QuestionAnswerRecordPort answerRecordPort) {
        this.statsRepositoryPort = statsRepositoryPort;
        this.answerRecordPort = answerRecordPort;
        this.commandGateway = commandGateway;
    }

    @EventHandler
    @Transactional
    public void on(GameEvent.QuestionAnsweredEvent event) {
        if (event.questionId() == null || event.playerId() == null) {
            return;
        }

        // Exclut les réponses synthétiques (bot et fantôme) : seule une réponse humaine compte.
        if (event.playerType() != GamePlayerType.HUMAN
                || QuizUpConstants.SYSTEM_USER_ID.equals(event.playerId())) {
            return;
        }

        // Dédup par clé métier (gameId, round, playerId) : un rejeu ne recompte pas la réponse.
        if (!answerRecordPort.record(
                event.gameId(),
                event.round().name(),
                event.playerId(),
                event.questionId(),
                event.correct())) {
            return;
        }

        QuestionAnswerStats stats = statsRepositoryPort.findById(event.questionId())
                .orElseGet(() -> QuestionAnswerStats.empty(event.questionId()))
                .withAnswer(event.correct());

        statsRepositoryPort.save(stats);

        QuestionDifficulty difficulty = QuestionRules.difficultyFor(
                stats.correctCount(),
                stats.answerCount()
        );

        commandGateway.send(new QuestionCommand.UpdateQuestionDifficultyCommand(
                event.questionId(),
                difficulty
        ));

        logger.debug("Difficulté projetée: questionId={}, correct={}/{}, difficulty={}",
                event.questionId(), stats.correctCount(), stats.answerCount(), difficulty);
    }
}
