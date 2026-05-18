package io.github.quizup.theme.domain.aggregate;

import io.github.quizup.theme.domain.command.TopicCommand;
import io.github.quizup.theme.domain.event.TopicEvent;
import io.github.quizup.theme.domain.exception.QuestionProblems;
import io.github.quizup.theme.domain.exception.TopicProblems;
import io.github.quizup.theme.domain.model.TopicCategory;
import io.github.quizup.theme.domain.model.TopicStatus;
import io.github.quizup.theme.domain.port.out.QuestionRepositoryPort;
import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.eventsourcing.EventSourcingHandler;
import org.axonframework.modelling.command.AggregateIdentifier;
import org.axonframework.modelling.command.AggregateLifecycle;
import org.axonframework.spring.stereotype.Aggregate;

import java.time.Instant;

import static io.github.quizup.theme.domain.model.TopicRules.MIN_QUESTIONS_TO_PUBLISH;

/**
 * TopicAggregate - Gère le cycle de vie d'un thème
 */
@Aggregate
public class TopicAggregate {

    @AggregateIdentifier
    private String topicId;
    private String name;
    private String description;
    private TopicCategory category;
    private TopicStatus status;
    private String creatorId;
    private Instant createdAt;
    private String updatedBy;
    private Instant updatedAt;

    // Constructeur par défaut requis par Axon
    protected TopicAggregate() {
    }

    @CommandHandler
    public TopicAggregate(TopicCommand.CreateTopicCommand command) {
        if (command.name() == null || command.name().isBlank()) {
            throw new TopicProblems.TopicNameEmptyProblem(command.topicId());
        }

        if (command.category() == null) {
            throw new TopicProblems.TopicCategoryEmptyProblem(command.topicId());
        }

        if (command.creatorId() == null || command.creatorId().isBlank()) {
            throw new TopicProblems.CreatorIdEmptyProblem(command.topicId());
        }

        AggregateLifecycle.apply(new TopicEvent.TopicCreatedEvent(
                command.topicId(),
                command.name(),
                command.description(),
                command.category(),
                command.creatorId(),
                Instant.now()
        ));
    }

    @CommandHandler
    public void handle(TopicCommand.PublishTopicCommand command, QuestionRepositoryPort questionRepositoryPort) {
        if (this.status != TopicStatus.DRAFT) {
            throw new TopicProblems.TopicNotInDraftProblem(this.topicId);
        }

        int approvedQuestionsCount = questionRepositoryPort.countApprovedByTopicId(this.topicId);

        if (approvedQuestionsCount < MIN_QUESTIONS_TO_PUBLISH) {
            throw new QuestionProblems.NotEnoughApprovedQuestionsProblem(
                    this.topicId, approvedQuestionsCount,
                    MIN_QUESTIONS_TO_PUBLISH
            );
        }

        AggregateLifecycle.apply(
                new TopicEvent.TopicPublishedEvent(
                        this.topicId,
                        command.requesterId(),
                        Instant.now()
                ));
    }

    @EventSourcingHandler
    public void on(TopicEvent.TopicCreatedEvent event) {
        this.topicId = event.topicId();
        this.name = event.name();
        this.description = event.description();
        this.category = event.category();
        this.status = TopicStatus.DRAFT;
        this.creatorId = event.creatorId();
        this.createdAt = event.createdAt();
        this.updatedBy = event.creatorId();
        this.updatedAt = event.createdAt();
    }

    @EventSourcingHandler
    public void on(TopicEvent.TopicPublishedEvent event) {
        this.status = TopicStatus.PUBLISHED;
        this.updatedBy = event.updatedBy();
        this.updatedAt = event.publishedAt();
    }
}
