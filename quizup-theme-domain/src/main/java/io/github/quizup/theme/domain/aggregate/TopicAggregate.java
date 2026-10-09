package io.github.quizup.theme.domain.aggregate;

import io.github.quizup.microservice.core.domain.model.i18n.Language;
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
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

import static io.github.quizup.theme.domain.model.TopicRules.MAX_COLOR_LENGTH;
import static io.github.quizup.theme.domain.model.TopicRules.MAX_DESCRIPTION_LENGTH;
import static io.github.quizup.theme.domain.model.TopicRules.MAX_EMOJI_LENGTH;
import static io.github.quizup.theme.domain.model.TopicRules.MAX_IMAGE_URL_LENGTH;
import static io.github.quizup.theme.domain.model.TopicRules.MAX_NAME_LENGTH;
import static io.github.quizup.theme.domain.model.TopicRules.MIN_QUESTIONS_TO_PUBLISH;

/**
 * TopicAggregate - Gère le cycle de vie d'un thème
 */
@Aggregate
public class TopicAggregate {

    @AggregateIdentifier
    private String topicId;
    private Map<Language, String> names = new EnumMap<>(Language.class);
    private String description;
    private TopicCategory category;
    private String emoji;
    private String color;
    private String imageUrl;
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
        validateNames(command.topicId(), command.names());
        validateDescription(command.topicId(), command.description());
        validateEmoji(command.topicId(), command.emoji());
        validateColor(command.topicId(), command.color());
        validateImageUrl(command.topicId(), command.imageUrl());

        if (command.category() == null) {
            throw new TopicProblems.TopicCategoryEmptyProblem(command.topicId());
        }

        if (command.creatorId() == null || command.creatorId().isBlank()) {
            throw new TopicProblems.CreatorIdEmptyProblem(command.topicId());
        }

        AggregateLifecycle.apply(new TopicEvent.TopicCreatedEvent(
                command.topicId(),
                new EnumMap<>(command.names()),
                command.description(),
                command.category(),
                command.emoji(),
                command.color(),
                command.imageUrl(),
                command.creatorId(),
                Instant.now()
        ));
    }

    @CommandHandler
    public void handle(TopicCommand.PublishTopicCommand command, QuestionRepositoryPort questionRepositoryPort) {
        requireOwner(command.requesterId());

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

    @CommandHandler
    public void handle(TopicCommand.UpdateTopicNameCommand command) {
        requireOwner(command.requestedBy());
        if (command.language() == null) {
            throw new TopicProblems.TopicNameEmptyProblem(command.topicId());
        }
        validateName(command.topicId(), command.name());
        if (Objects.equals(command.name(), this.names.get(command.language()))) {
            return;
        }

        AggregateLifecycle.apply(new TopicEvent.TopicNameUpdatedEvent(
                command.topicId(), command.requestedBy(), command.language(), command.name(), Instant.now()));
    }

    @CommandHandler
    public void handle(TopicCommand.UpdateTopicDescriptionCommand command) {
        requireOwner(command.requestedBy());
        validateDescription(command.topicId(), command.description());
        if (Objects.equals(command.description(), this.description)) {
            return;
        }

        AggregateLifecycle.apply(new TopicEvent.TopicDescriptionUpdatedEvent(
                command.topicId(), command.requestedBy(), command.description(), Instant.now()));
    }

    @CommandHandler
    public void handle(TopicCommand.UpdateTopicCategoryCommand command) {
        requireOwner(command.requestedBy());
        if (command.category() == null) {
            throw new TopicProblems.TopicCategoryEmptyProblem(command.topicId());
        }
        if (command.category() == this.category) {
            return;
        }

        AggregateLifecycle.apply(new TopicEvent.TopicCategoryUpdatedEvent(
                command.topicId(), command.requestedBy(), command.category(), Instant.now()));
    }

    @CommandHandler
    public void handle(TopicCommand.UpdateTopicEmojiCommand command) {
        requireOwner(command.requestedBy());
        validateEmoji(command.topicId(), command.emoji());
        if (Objects.equals(command.emoji(), this.emoji)) {
            return;
        }

        AggregateLifecycle.apply(new TopicEvent.TopicEmojiUpdatedEvent(
                command.topicId(), command.requestedBy(), command.emoji(), Instant.now()));
    }

    @CommandHandler
    public void handle(TopicCommand.UpdateTopicColorCommand command) {
        requireOwner(command.requestedBy());
        validateColor(command.topicId(), command.color());
        if (Objects.equals(command.color(), this.color)) {
            return;
        }

        AggregateLifecycle.apply(new TopicEvent.TopicColorUpdatedEvent(
                command.topicId(), command.requestedBy(), command.color(), Instant.now()));
    }

    @CommandHandler
    public void handle(TopicCommand.UpdateTopicImageUrlCommand command) {
        requireOwner(command.requestedBy());
        validateImageUrl(command.topicId(), command.imageUrl());
        if (Objects.equals(command.imageUrl(), this.imageUrl)) {
            return;
        }

        AggregateLifecycle.apply(new TopicEvent.TopicImageUrlUpdatedEvent(
                command.topicId(), command.requestedBy(), command.imageUrl(), Instant.now()));
    }

    @EventSourcingHandler
    public void on(TopicEvent.TopicCreatedEvent event) {
        this.topicId = event.topicId();
        this.names = new EnumMap<>(event.names());
        this.description = event.description();
        this.category = event.category();
        this.emoji = event.emoji();
        this.color = event.color();
        this.imageUrl = event.imageUrl();
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

    @EventSourcingHandler
    public void on(TopicEvent.TopicNameUpdatedEvent event) {
        if (this.names == null) {
            this.names = new EnumMap<>(Language.class);
        }
        this.names.put(event.language(), event.name());
        this.updatedBy = event.updatedBy();
        this.updatedAt = event.updatedAt();
    }

    @EventSourcingHandler
    public void on(TopicEvent.TopicDescriptionUpdatedEvent event) {
        this.description = event.description();
        this.updatedBy = event.updatedBy();
        this.updatedAt = event.updatedAt();
    }

    @EventSourcingHandler
    public void on(TopicEvent.TopicCategoryUpdatedEvent event) {
        this.category = event.category();
        this.updatedBy = event.updatedBy();
        this.updatedAt = event.updatedAt();
    }

    @EventSourcingHandler
    public void on(TopicEvent.TopicEmojiUpdatedEvent event) {
        this.emoji = event.emoji();
        this.updatedBy = event.updatedBy();
        this.updatedAt = event.updatedAt();
    }

    @EventSourcingHandler
    public void on(TopicEvent.TopicColorUpdatedEvent event) {
        this.color = event.color();
        this.updatedBy = event.updatedBy();
        this.updatedAt = event.updatedAt();
    }

    @EventSourcingHandler
    public void on(TopicEvent.TopicImageUrlUpdatedEvent event) {
        this.imageUrl = event.imageUrl();
        this.updatedBy = event.updatedBy();
        this.updatedAt = event.updatedAt();
    }

    private void requireOwner(String requestedBy) {
        if (!Objects.equals(this.creatorId, requestedBy)) {
            throw new TopicProblems.TopicNotOwnerProblem(this.topicId, requestedBy);
        }
    }

    private static void validateNames(String topicId, Map<Language, String> names) {
        if (names == null || names.isEmpty() || names.get(Language.FR) == null || names.get(Language.FR).isBlank()) {
            throw new TopicProblems.TopicNameEmptyProblem(topicId);
        }
        names.forEach((language, name) -> {
            if (language == null) {
                throw new TopicProblems.TopicNameEmptyProblem(topicId);
            }
            validateName(topicId, name);
        });
    }

    private static void validateName(String topicId, String name) {
        if (name == null || name.isBlank()) {
            throw new TopicProblems.TopicNameEmptyProblem(topicId);
        }
        if (name.length() > MAX_NAME_LENGTH) {
            throw new TopicProblems.TopicNameTooLongProblem(topicId, MAX_NAME_LENGTH);
        }
    }

    private static void validateDescription(String topicId, String description) {
        if (description != null && description.length() > MAX_DESCRIPTION_LENGTH) {
            throw new TopicProblems.TopicDescriptionTooLongProblem(topicId, MAX_DESCRIPTION_LENGTH);
        }
    }

    private static void validateEmoji(String topicId, String emoji) {
        if (emoji != null && emoji.length() > MAX_EMOJI_LENGTH) {
            throw new TopicProblems.TopicEmojiTooLongProblem(topicId, MAX_EMOJI_LENGTH);
        }
    }

    private static void validateColor(String topicId, String color) {
        if (color != null && color.length() > MAX_COLOR_LENGTH) {
            throw new TopicProblems.TopicColorTooLongProblem(topicId, MAX_COLOR_LENGTH);
        }
    }

    private static void validateImageUrl(String topicId, String imageUrl) {
        if (imageUrl != null && imageUrl.length() > MAX_IMAGE_URL_LENGTH) {
            throw new TopicProblems.TopicImageUrlTooLongProblem(topicId, MAX_IMAGE_URL_LENGTH);
        }
    }

}
