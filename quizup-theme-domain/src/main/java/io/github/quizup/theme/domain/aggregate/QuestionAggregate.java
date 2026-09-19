package io.github.quizup.theme.domain.aggregate;

import io.github.quizup.theme.domain.command.QuestionCommand;
import io.github.quizup.theme.domain.event.QuestionEvent;
import io.github.quizup.theme.domain.exception.QuestionProblems;
import io.github.quizup.theme.domain.model.QuestionChoice;
import io.github.quizup.theme.domain.model.QuestionDifficulty;
import io.github.quizup.theme.domain.model.QuestionStatus;
import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.eventsourcing.EventSourcingHandler;
import org.axonframework.modelling.command.AggregateIdentifier;
import org.axonframework.modelling.command.AggregateLifecycle;
import org.axonframework.spring.stereotype.Aggregate;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

@Aggregate
public class QuestionAggregate {

    @AggregateIdentifier
    private String questionId;
    private String topicId;

    /**
     * TODO
     * private Map<Language, String> textByLanguage;
     * private Map<Choice, Map<Language, String>> choiceByLanguage;
     **/

    private String text;
    private Map<QuestionChoice, String> answers;
    private QuestionChoice correctAnswer;
    private QuestionStatus status;
    private QuestionDifficulty difficulty;
    private String imageUrl;
    private String creatorId;
    private Instant createdAt;
    private String updatedBy;
    private Instant updatedAt;

    // Constructeur par défaut requis par Axon
    protected QuestionAggregate() {
    }

    @CommandHandler
    public QuestionAggregate(QuestionCommand.CreateQuestionCommand command) {
        validateQuestionData(command.questionId(), command.text(), command.answers(), command.correctAnswer());

        AggregateLifecycle.apply(
                new QuestionEvent.QuestionCreatedEvent(
                        command.questionId(),
                        command.topicId(),
                        command.text(),
                        command.answers(),
                        command.correctAnswer(),
                        command.imageUrl(),
                        command.creatorId(),
                        Instant.now()
                ));
    }

    @CommandHandler
    public void handle(QuestionCommand.ApproveQuestionCommand command) {
        if (this.status == QuestionStatus.APPROVED) {
            throw new QuestionProblems.QuestionAlreadyApprovedProblem(command.questionId());
        }

        AggregateLifecycle.apply(
                new QuestionEvent.QuestionApprovedEvent(
                        command.questionId(),
                        this.topicId,
                        this.status,
                        command.requesterId(),
                        Instant.now()
                ));
    }

    @CommandHandler
    public void handle(QuestionCommand.RejectQuestionCommand command) {
        if (this.status == QuestionStatus.REJECTED) {
            throw new QuestionProblems.QuestionAlreadyRejectedProblem(command.questionId());
        }

        AggregateLifecycle.apply(
                new QuestionEvent.QuestionRejectedEvent(
                        command.questionId(),
                        this.topicId,
                        this.status,
                        command.reason(),
                        command.requesterId(),
                        Instant.now()
                ));
    }

    /**
     * Met à jour la difficulté déduite. Idempotent : aucune commande n'émet d'événement si la
     * difficulté est inchangée (le recalcul peut être déclenché à chaque réponse).
     */
    @CommandHandler
    public void handle(QuestionCommand.UpdateQuestionDifficultyCommand command) {
        if (Objects.equals(this.difficulty, command.difficulty())) {
            return;
        }

        AggregateLifecycle.apply(
                new QuestionEvent.QuestionDifficultyUpdatedEvent(
                        command.questionId(),
                        command.difficulty(),
                        Instant.now()
                ));
    }

    @EventSourcingHandler
    public void on(QuestionEvent.QuestionCreatedEvent event) {
        this.questionId = event.questionId();
        this.topicId = event.topicId();
        this.text = event.text();
        this.imageUrl = event.imageUrl();
        this.answers = event.answers();
        this.correctAnswer = event.correctAnswer();
        this.status = QuestionStatus.PENDING;
        this.creatorId = event.creatorId();
        this.createdAt = event.createdAt();
        this.updatedBy = event.creatorId();
        this.updatedAt = event.createdAt();
    }

    @EventSourcingHandler
    public void on(QuestionEvent.QuestionApprovedEvent event) {
        this.status = QuestionStatus.APPROVED;
        this.updatedBy = event.updatedBy();
        this.updatedAt = event.approvedAt();
    }

    @EventSourcingHandler
    public void on(QuestionEvent.QuestionRejectedEvent event) {
        this.status = QuestionStatus.REJECTED;
        this.updatedBy = event.updatedBy();
        this.updatedAt = event.rejectedAt();
    }

    @EventSourcingHandler
    public void on(QuestionEvent.QuestionDifficultyUpdatedEvent event) {
        this.difficulty = event.difficulty();
        this.updatedAt = event.updatedAt();
    }

    private static void validateQuestionData(String questionId,
                                             String text,
                                             Map<QuestionChoice, String> answers,
                                             QuestionChoice correctAnswer) {
        if (text == null || text.isBlank()) {
            throw new QuestionProblems.QuestionTextEmptyProblem(questionId);
        }
        if (answers == null || answers.size() != 4) {
            throw new QuestionProblems.QuestionAnswersInvalidProblem(questionId);
        }
        if (correctAnswer == null || !answers.containsKey(correctAnswer)) {
            throw new QuestionProblems.QuestionCorrectAnswerMissingProblem(questionId);
        }
    }
}
