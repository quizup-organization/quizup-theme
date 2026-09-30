package io.github.quizup.theme.domain.aggregate;

import io.github.quizup.microservice.core.domain.model.i18n.Language;
import io.github.quizup.theme.domain.command.QuestionCommand;
import io.github.quizup.theme.domain.event.QuestionEvent;
import io.github.quizup.theme.domain.exception.QuestionProblems;
import io.github.quizup.theme.domain.model.QuestionChoice;
import io.github.quizup.theme.domain.model.QuestionContent;
import io.github.quizup.theme.domain.model.QuestionDifficulty;
import io.github.quizup.theme.domain.model.QuestionStatus;
import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.eventsourcing.EventSourcingHandler;
import org.axonframework.modelling.command.AggregateIdentifier;
import org.axonframework.modelling.command.AggregateLifecycle;
import org.axonframework.spring.stereotype.Aggregate;

import java.time.Instant;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/**
 * Agrégat multilingue d'une question : un contenu par langue disponible (FR, EN, ou les deux).
 * Aucune langue source explicite : la disponibilité est portée par les clés de
 * {@link #contents}.
 */
@Aggregate
public class QuestionAggregate {

    @AggregateIdentifier
    private String questionId;
    private String topicId;

    /** Contenus localisés disponibles (langue → contenu). */
    private Map<Language, QuestionContent> contents = new EnumMap<>(Language.class);

    private String imageUrl;
    private QuestionChoice correctAnswer;
    private QuestionStatus status;
    private QuestionDifficulty difficulty;
    private String creatorId;
    private Instant createdAt;
    private String updatedBy;
    private Instant updatedAt;

    // Constructeur par défaut requis par Axon
    protected QuestionAggregate() {
    }

    @CommandHandler
    public QuestionAggregate(QuestionCommand.CreateQuestionCommand command) {
        validateContents(command.questionId(), command.contents(), command.correctAnswer());

        AggregateLifecycle.apply(
                new QuestionEvent.QuestionCreatedEvent(
                        command.questionId(),
                        command.topicId(),
                        command.contents(),
                        command.correctAnswer(),
                        command.imageUrl(),
                        command.creatorId(),
                        Instant.now()
                ));
    }

    /**
     * Ajoute (ou remplace) des contenus localisés. Idempotent : aucun événement si tous les
     * contenus fournis sont déjà identiques.
     */
    @CommandHandler
    public void handle(QuestionCommand.AddQuestionTranslationsCommand command) {
        validateContents(command.questionId(), command.contents(), this.correctAnswer);

        Map<Language, QuestionContent> changed = new EnumMap<>(Language.class);
        for (Map.Entry<Language, QuestionContent> entry : command.contents().entrySet()) {
            if (!Objects.equals(this.contents.get(entry.getKey()), entry.getValue())) {
                changed.put(entry.getKey(), entry.getValue());
            }
        }
        if (changed.isEmpty()) {
            return;
        }

        AggregateLifecycle.apply(
                new QuestionEvent.QuestionTranslationsAddedEvent(
                        command.questionId(),
                        changed,
                        command.updatedBy(),
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
        this.contents = new EnumMap<>(Language.class);
        this.contents.putAll(event.contents());
        this.imageUrl = event.imageUrl();
        this.correctAnswer = event.correctAnswer();
        this.status = QuestionStatus.PENDING;
        this.creatorId = event.creatorId();
        this.createdAt = event.createdAt();
        this.updatedBy = event.creatorId();
        this.updatedAt = event.createdAt();
    }

    @EventSourcingHandler
    public void on(QuestionEvent.QuestionTranslationsAddedEvent event) {
        if (this.contents == null) {
            this.contents = new EnumMap<>(Language.class);
        }
        this.contents.putAll(event.contents());
        this.updatedBy = event.updatedBy();
        this.updatedAt = event.updatedAt();
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

    private static void validateContents(String questionId,
                                         Map<Language, QuestionContent> contents,
                                         QuestionChoice correctAnswer) {
        if (contents == null || contents.isEmpty()) {
            throw new QuestionProblems.QuestionContentsEmptyProblem(questionId);
        }
        for (Map.Entry<Language, QuestionContent> entry : contents.entrySet()) {
            validateContent(questionId, entry.getKey(), entry.getValue(), correctAnswer);
        }
    }

    private static void validateContent(String questionId,
                                        Language language,
                                        QuestionContent content,
                                        QuestionChoice correctAnswer) {
        if (language == null || content == null || content.language() != language) {
            throw new QuestionProblems.QuestionContentLanguageMismatchProblem(
                    questionId, language, content == null ? null : content.language());
        }
        if (content.text() == null || content.text().isBlank()) {
            throw new QuestionProblems.QuestionTextEmptyProblem(questionId);
        }
        if (content.answers() == null || content.answers().size() != 4) {
            throw new QuestionProblems.QuestionAnswersInvalidProblem(questionId);
        }
        if (correctAnswer == null || !content.answers().containsKey(correctAnswer)) {
            throw new QuestionProblems.QuestionCorrectAnswerMissingProblem(questionId);
        }
    }
}
