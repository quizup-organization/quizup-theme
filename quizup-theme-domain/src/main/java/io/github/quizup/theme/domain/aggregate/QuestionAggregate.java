package io.github.quizup.theme.domain.aggregate;

import io.github.quizup.microservice.core.domain.model.i18n.Language;
import io.github.quizup.theme.domain.command.QuestionCommand;
import io.github.quizup.theme.domain.event.QuestionEvent;
import io.github.quizup.theme.domain.exception.QuestionProblems;
import io.github.quizup.theme.domain.model.QuestionChoice;
import io.github.quizup.theme.domain.model.QuestionContent;
import io.github.quizup.theme.domain.model.QuestionDifficulty;
import io.github.quizup.theme.domain.model.QuestionRules;
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
    public void handle(QuestionCommand.AddQuestionTranslationCommand command) {
        if (command.language() == null) {
            throw new QuestionProblems.QuestionLanguageMissingProblem(command.questionId());
        }
        if (this.contents.containsKey(command.language())) {
            throw new QuestionProblems.QuestionTranslationAlreadyExistsProblem(
                    command.questionId(), command.language());
        }

        validateContent(command.questionId(), command.language(),
                new QuestionContent(command.language(), command.text(), command.answers()),
                this.correctAnswer);

        AggregateLifecycle.apply(
                new QuestionEvent.QuestionTranslationAddedEvent(
                        command.questionId(),
                        command.language(),
                        command.text(),
                        command.answers(),
                        command.requestedBy(),
                        Instant.now()
                ));
    }

    @CommandHandler
    public void handle(QuestionCommand.UpdateQuestionTextCommand command) {
        QuestionContent content = requireContent(command.questionId(), command.language());
        validateText(command.questionId(), command.text());

        if (Objects.equals(command.text(), content.text())) {
            return;
        }

        AggregateLifecycle.apply(
                new QuestionEvent.QuestionTextUpdatedEvent(
                        command.questionId(),
                        command.language(),
                        command.text(),
                        command.requestedBy(),
                        Instant.now()
                ));
    }

    @CommandHandler
    public void handle(QuestionCommand.UpdateQuestionAnswersCommand command) {
        QuestionContent content = requireContent(command.questionId(), command.language());
        validateAnswers(command.questionId(), command.answers(), this.correctAnswer);

        if (Objects.equals(command.answers(), content.answers())) {
            return;
        }

        AggregateLifecycle.apply(
                new QuestionEvent.QuestionAnswersUpdatedEvent(
                        command.questionId(),
                        command.language(),
                        command.answers(),
                        command.requestedBy(),
                        Instant.now()
                ));
    }

    @CommandHandler
    public void handle(QuestionCommand.UpdateQuestionCorrectAnswerCommand command) {
        validateAnswers(command.questionId(), null, command.correctAnswer());
        for (QuestionContent content : this.contents.values()) {
            if (!content.answers().containsKey(command.correctAnswer())) {
                throw new QuestionProblems.QuestionCorrectAnswerMissingProblem(command.questionId());
            }
        }

        if (command.correctAnswer() == this.correctAnswer) {
            return;
        }

        AggregateLifecycle.apply(
                new QuestionEvent.QuestionCorrectAnswerUpdatedEvent(
                        command.questionId(),
                        command.correctAnswer(),
                        command.requestedBy(),
                        Instant.now()
                ));
    }

    @CommandHandler
    public void handle(QuestionCommand.UpdateQuestionImageUrlCommand command) {
        if (command.imageUrl() != null && command.imageUrl().length() > QuestionRules.MAX_IMAGE_URL_LENGTH) {
            throw new QuestionProblems.QuestionImageUrlTooLongProblem(
                    command.questionId(), QuestionRules.MAX_IMAGE_URL_LENGTH);
        }

        if (Objects.equals(command.imageUrl(), this.imageUrl)) {
            return;
        }

        AggregateLifecycle.apply(
                new QuestionEvent.QuestionImageUrlUpdatedEvent(
                        command.questionId(),
                        command.imageUrl(),
                        command.requestedBy(),
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
    public void on(QuestionEvent.QuestionTranslationAddedEvent event) {
        this.contents.put(event.language(),
                new QuestionContent(event.language(), event.text(), event.answers()));
        this.updatedBy = event.updatedBy();
        this.updatedAt = event.updatedAt();
    }

    @EventSourcingHandler
    public void on(QuestionEvent.QuestionTextUpdatedEvent event) {
        QuestionContent previous = this.contents.get(event.language());
        this.contents.put(event.language(),
                new QuestionContent(event.language(), event.text(), previous.answers()));
        this.updatedBy = event.updatedBy();
        this.updatedAt = event.updatedAt();
    }

    @EventSourcingHandler
    public void on(QuestionEvent.QuestionAnswersUpdatedEvent event) {
        QuestionContent previous = this.contents.get(event.language());
        this.contents.put(event.language(),
                new QuestionContent(event.language(), previous.text(), event.answers()));
        this.updatedBy = event.updatedBy();
        this.updatedAt = event.updatedAt();
    }

    @EventSourcingHandler
    public void on(QuestionEvent.QuestionCorrectAnswerUpdatedEvent event) {
        this.correctAnswer = event.correctAnswer();
        this.updatedBy = event.updatedBy();
        this.updatedAt = event.updatedAt();
    }

    @EventSourcingHandler
    public void on(QuestionEvent.QuestionImageUrlUpdatedEvent event) {
        this.imageUrl = event.imageUrl();
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

    private QuestionContent requireContent(String questionId, Language language) {
        if (language == null) {
            throw new QuestionProblems.QuestionLanguageMissingProblem(questionId);
        }
        QuestionContent content = this.contents.get(language);
        if (content == null) {
            throw new QuestionProblems.QuestionLanguageNotFoundProblem(questionId, language);
        }
        return content;
    }

    private static void validateText(String questionId, String text) {
        if (text == null || text.isBlank()) {
            throw new QuestionProblems.QuestionTextEmptyProblem(questionId);
        }
        if (text.length() > QuestionRules.MAX_TEXT_LENGTH) {
            throw new QuestionProblems.QuestionTextTooLongProblem(questionId, QuestionRules.MAX_TEXT_LENGTH);
        }
    }

    private static void validateAnswers(String questionId,
                                        Map<QuestionChoice, String> answers,
                                        QuestionChoice correctAnswer) {
        if (answers != null && answers.size() != 4) {
            throw new QuestionProblems.QuestionAnswersInvalidProblem(questionId);
        }
        if (correctAnswer == null || (answers != null && !answers.containsKey(correctAnswer))) {
            throw new QuestionProblems.QuestionCorrectAnswerMissingProblem(questionId);
        }
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
        validateText(questionId, content.text());
        if (content.answers() == null || content.answers().size() != 4) {
            throw new QuestionProblems.QuestionAnswersInvalidProblem(questionId);
        }
        if (correctAnswer == null || !content.answers().containsKey(correctAnswer)) {
            throw new QuestionProblems.QuestionCorrectAnswerMissingProblem(questionId);
        }
    }
}
