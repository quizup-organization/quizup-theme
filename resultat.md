### ./quizup-theme-domain/src/main/java/io/github/quizup/theme/domain/aggregate/QuestionAggregate.java
```java
package io.github.quizup.theme.domain.aggregate;

import io.github.quizup.theme.domain.command.QuestionCommand;
import io.github.quizup.theme.domain.event.QuestionEvent;
import io.github.quizup.theme.domain.exception.QuestionProblems;
import io.github.quizup.theme.domain.model.QuestionChoice;
import io.github.quizup.theme.domain.model.QuestionStatus;
import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.eventsourcing.EventSourcingHandler;
import org.axonframework.modelling.command.AggregateIdentifier;
import org.axonframework.modelling.command.AggregateLifecycle;
import org.axonframework.spring.stereotype.Aggregate;

import java.time.Instant;
import java.util.Map;

@Aggregate
public class QuestionAggregate {

    @AggregateIdentifier
    private String questionId;
    private String topicId;

    /**
     * TODO
     * private Map<Language, String> textByLanguage;
     * private Map<Choice, Map<Language, String>> choiceByLanguage;
     * private Difficulty declaredDifficulty;   // fixée manuellement
     * private Difficulty computedDifficulty;   // calculée automatiquement
     **/

    private String text;
    private Map<QuestionChoice, String> answers;
    private QuestionChoice correctAnswer;
    private QuestionStatus status;
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

    @EventSourcingHandler
    public void on(QuestionEvent.QuestionCreatedEvent event) {
        this.questionId = event.questionId();
        this.topicId = event.topicId();
        this.text = event.text();
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
```

### ./quizup-theme-domain/src/main/java/io/github/quizup/theme/domain/aggregate/TopicAggregate.java
```java
package io.github.quizup.theme.domain.aggregate;

import io.github.quizup.theme.domain.command.TopicCommand;
import io.github.quizup.theme.domain.event.TopicEvent;
import io.github.quizup.theme.domain.exception.QuestionProblems;
import io.github.quizup.theme.domain.exception.TopicProblems;
import io.github.quizup.theme.domain.model.QuestionStatus;
import io.github.quizup.theme.domain.model.TopicCategory;
import io.github.quizup.theme.domain.model.TopicStatus;
import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.eventsourcing.EventSourcingHandler;
import org.axonframework.modelling.command.AggregateIdentifier;
import org.axonframework.modelling.command.AggregateLifecycle;
import org.axonframework.spring.stereotype.Aggregate;

import java.time.Instant;
import java.util.EnumMap;
import java.util.Map;

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
    private Integer followersCounter;
    private Map<QuestionStatus, Integer> questionsCounter;
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
    public void handle(TopicCommand.PublishTopicCommand command) {
        if (this.status != TopicStatus.DRAFT) {
            throw new TopicProblems.TopicNotInDraftProblem(this.topicId);
        }

        int approvedQuestionsCount = questionsCounter.get(QuestionStatus.APPROVED);

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
    public void handle(TopicCommand.IncreaseFollowersCounterCommand command) {
        AggregateLifecycle.apply(
                new TopicEvent.TopicFollowersCounterIncreasedEvent(
                        this.topicId,
                        Instant.now()
                ));
    }

    @CommandHandler
    public void handle(TopicCommand.DecreaseFollowersCounterCommand command) {
        if (followersCounter <= 0) {
            throw new TopicProblems.TopicFollowersCounterUnderflowProblem(this.topicId);
        }

        AggregateLifecycle.apply(
                new TopicEvent.TopicFollowersCounterDecreasedEvent(
                        this.topicId,
                        Instant.now()
                ));
    }

    @CommandHandler
    public void handle(TopicCommand.IncreaseQuestionsCounterCommand command) {
        if (command.questionStatus() == null) {
            throw new TopicProblems.TopicQuestionStatusRequiredProblem(this.topicId);
        }

        AggregateLifecycle.apply(
                new TopicEvent.TopicQuestionsCounterIncreasedEvent(
                        this.topicId,
                        command.questionStatus(),
                        Instant.now()
                ));
    }

    @CommandHandler
    public void handle(TopicCommand.DecreaseQuestionsCounterCommand command) {
        if (command.questionStatus() == null) {
            throw new TopicProblems.TopicQuestionStatusRequiredProblem(this.topicId);
        }

        int current = questionsCounter.get(command.questionStatus());

        if (current <= 0) {
            throw new TopicProblems.TopicQuestionsCounterUnderflowProblem(this.topicId, command.questionStatus());
        }

        AggregateLifecycle.apply(
                new TopicEvent.TopicQuestionsCounterDecreasedEvent(
                        this.topicId,
                        command.questionStatus(),
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
        this.followersCounter = 0;
        this.questionsCounter = new EnumMap<>(QuestionStatus.class);
        this.questionsCounter.put(QuestionStatus.PENDING, 0);
        this.questionsCounter.put(QuestionStatus.APPROVED, 0);
        this.questionsCounter.put(QuestionStatus.REJECTED, 0);
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
    public void on(TopicEvent.TopicFollowersCounterIncreasedEvent event) {
        this.followersCounter += 1;
        this.updatedAt = event.updatedAt();
    }

    @EventSourcingHandler
    public void on(TopicEvent.TopicFollowersCounterDecreasedEvent event) {
        this.followersCounter -= 1;
        this.updatedAt = event.updatedAt();
    }

    @EventSourcingHandler
    public void on(TopicEvent.TopicQuestionsCounterIncreasedEvent event) {
        this.questionsCounter.computeIfPresent(event.questionStatus(), (_, count) -> count + 1);
        this.updatedAt = event.updatedAt();
    }

    @EventSourcingHandler
    public void on(TopicEvent.TopicQuestionsCounterDecreasedEvent event) {
        this.questionsCounter.computeIfPresent(event.questionStatus(), (_, count) -> count - 1);
        this.updatedAt = event.updatedAt();
    }
}
```

### ./quizup-theme-domain/src/main/java/io/github/quizup/theme/domain/command/QuestionCommand.java
```java
package io.github.quizup.theme.domain.command;

import io.github.quizup.theme.domain.model.QuestionChoice;
import org.axonframework.modelling.command.TargetAggregateIdentifier;

import java.util.Map;

public interface QuestionCommand {
    String questionId();

    /**
     * Commande pour ajouter une question à un thème
     */
    record CreateQuestionCommand(
            @TargetAggregateIdentifier String questionId,
            String topicId,
            String text,
            Map<QuestionChoice, String> answers,
            QuestionChoice correctAnswer,
            String creatorId
    ) implements QuestionCommand {
    }

    /**
     * Commande pour approuver une question (PENDING -> APPROVED)
     */
    record ApproveQuestionCommand(
            @TargetAggregateIdentifier String questionId,
            String requesterId
    ) implements QuestionCommand {
    }


    /**
     * Commande pour rejeter une question (PENDING -> REJECTED)
     */
    record RejectQuestionCommand(
            @TargetAggregateIdentifier String questionId,
            String requesterId,
            String reason
    ) implements QuestionCommand {
    }
}

```

### ./quizup-theme-domain/src/main/java/io/github/quizup/theme/domain/command/TopicCommand.java
```java
package io.github.quizup.theme.domain.command;

import io.github.quizup.theme.domain.model.QuestionStatus;
import io.github.quizup.theme.domain.model.TopicCategory;
import org.axonframework.modelling.command.TargetAggregateIdentifier;

public interface TopicCommand {
    String topicId();

    /**
     * Commande pour créer un nouveau thème de quiz
     */
    record CreateTopicCommand(
            @TargetAggregateIdentifier String topicId,
            String name,
            String description,
            TopicCategory category,
            String creatorId
    ) implements TopicCommand {
    }

    /**
     * Commande pour publier un thème (transition DRAFT -> PUBLISHED)
     * Requiert au minimum 7 questions approuvées
     */
    record PublishTopicCommand(
            @TargetAggregateIdentifier String topicId,
            String requesterId
    ) implements TopicCommand {
    }

    record IncreaseFollowersCounterCommand(
            @TargetAggregateIdentifier String topicId
            ) implements TopicCommand {
    }

    record DecreaseFollowersCounterCommand(
            @TargetAggregateIdentifier String topicId
    ) implements TopicCommand {
    }

    record IncreaseQuestionsCounterCommand(
            @TargetAggregateIdentifier String topicId,
            QuestionStatus questionStatus
    ) implements TopicCommand {
    }

    record DecreaseQuestionsCounterCommand(
            @TargetAggregateIdentifier String topicId,
            QuestionStatus questionStatus
    ) implements TopicCommand {
    }
}

```

### ./quizup-theme-domain/src/main/java/io/github/quizup/theme/domain/event/QuestionEvent.java
```java
package io.github.quizup.theme.domain.event;


import io.github.quizup.theme.domain.model.QuestionChoice;
import io.github.quizup.theme.domain.model.QuestionStatus;

import java.time.Instant;
import java.util.Map;

public interface QuestionEvent {
    String questionId();

    /**
     * Événement émis lors de l'ajout d'une question à un thème
     */
    record QuestionCreatedEvent(
            String questionId,
            String topicId,
            String text,
            Map<QuestionChoice, String> answers,
            QuestionChoice correctAnswer,
            String creatorId,
            Instant createdAt
    ) implements QuestionEvent {
    }

    /**
     * Événement émis lors de l'approbation d'une question
     */
    record QuestionApprovedEvent(
            String questionId,
            String topicId,
            QuestionStatus previousStatus,
            String updatedBy,
            Instant approvedAt
    ) implements QuestionEvent {
    }

    /**
     * Événement émis lors du rejet d'une question
     */
    record QuestionRejectedEvent(
            String questionId,
            String topicId,
            QuestionStatus previousStatus,
            String reason,
            String updatedBy,
            Instant rejectedAt
    ) implements QuestionEvent {
    }
}

```

### ./quizup-theme-domain/src/main/java/io/github/quizup/theme/domain/event/TopicEvent.java
```java
package io.github.quizup.theme.domain.event;

import io.github.quizup.theme.domain.model.QuestionStatus;
import io.github.quizup.theme.domain.model.TopicCategory;

import java.time.Instant;

public interface TopicEvent {
    String topicId();

    /**
     * Événement émis lors de la création d'un thème
     */
    record TopicCreatedEvent(
            String topicId,
            String name,
            String description,
            TopicCategory category,
            String creatorId,
            Instant createdAt
    ) implements TopicEvent {
    }

    /**
     * Événement émis lors de la publication d'un thème (DRAFT -> PUBLISHED)
     */
    record TopicPublishedEvent(
            String topicId,
            String updatedBy,
            Instant publishedAt
    ) implements TopicEvent {
    }

    record TopicFollowersCounterIncreasedEvent(
            String topicId,
            Instant updatedAt
    ) implements TopicEvent {
    }

    record TopicFollowersCounterDecreasedEvent(
            String topicId,
            Instant updatedAt
    ) implements TopicEvent {
    }

    record TopicQuestionsCounterIncreasedEvent(
            String topicId,
            QuestionStatus questionStatus,
            Instant updatedAt
    ) implements TopicEvent {
    }
    record TopicQuestionsCounterDecreasedEvent(
            String topicId,
            QuestionStatus questionStatus,
            Instant updatedAt
    ) implements TopicEvent {
    }
}

```

### ./quizup-theme-domain/src/main/java/io/github/quizup/theme/domain/exception/QuestionProblem.java
```java
package io.github.quizup.theme.domain.exception;

import io.github.quizup.common.domain.exception.BaseProblem;
import io.github.quizup.common.domain.exception.ProblemCategory;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

/**
 * Classe de base pour toutes les exceptions métier liées au domaine Question
 */
@Getter
public abstract class QuestionProblem extends BaseProblem {

    private final String questionId;

    protected QuestionProblem(
            String questionId,
            String type,
            ProblemCategory category,
            String title,
            String detail,
            Map<String, Object> context) {
        super(type, category, title, detail, mergeContext(context, questionId));
        this.questionId = questionId;
    }

    protected QuestionProblem(
            String questionId,
            String type,
            String title,
            String detail,
            Map<String, Object> context) {
        this(questionId, type, ProblemCategory.BUSINESS_INVALID_COMMAND, title, detail, context);
    }

    protected QuestionProblem(
            String questionId,
            String type,
            String title,
            String detail) {
        this(questionId, type, ProblemCategory.BUSINESS_INVALID_COMMAND, title, detail, null);
    }

    protected QuestionProblem(
            String questionId,
            String type,
            String title) {
        this(questionId, type, title, null, null);
    }

    private static Map<String, Object> mergeContext(Map<String, Object> context, String questionId) {
        Map<String, Object> merged = new HashMap<>();
        if (context != null) {
            merged.putAll(context);
        }
        merged.put("questionId", questionId);
        return merged;
    }

}

```

### ./quizup-theme-domain/src/main/java/io/github/quizup/theme/domain/exception/QuestionProblems.java
```java
package io.github.quizup.theme.domain.exception;

import io.github.quizup.common.domain.exception.ProblemCategory;

import java.util.Map;

/**
 * Exceptions spécifiques au domaine Question
 */
public final class QuestionProblems {

    private QuestionProblems() {
        // Classe utilitaire
    }

    public static class QuestionNotFoundProblem extends QuestionProblem {
        public QuestionNotFoundProblem(String questionId) {
            super(questionId, "urn:quizup:question:notFound",
                    ProblemCategory.BUSINESS_RESOURCE_MISSING,
                    "Question not found",
                    "The question " + questionId + " was not found", null);
        }
    }

    public static class QuestionTextEmptyProblem extends QuestionProblem {
        public QuestionTextEmptyProblem(String questionId) {
            super(questionId, "urn:quizup:question:textEmpty",
                    "Question text cannot be empty",
                    "The question text must not be empty");
        }
    }

    public static class QuestionAnswersInvalidProblem extends QuestionProblem {
        public QuestionAnswersInvalidProblem(String questionId) {
            super(questionId, "urn:quizup:question:answersInvalid",
                    "Invalid answers",
                    "A question must have exactly 4 answers (A, B, C, D)");
        }
    }

    public static class QuestionCorrectAnswerMissingProblem extends QuestionProblem {
        public QuestionCorrectAnswerMissingProblem(String questionId) {
            super(questionId, "urn:quizup:question:correctAnswerMissing",
                    "Correct answer missing",
                    "The correct answer must be provided and match one of the choices");
        }
    }

    public static class QuestionAlreadyApprovedProblem extends QuestionProblem {
        public QuestionAlreadyApprovedProblem(String questionId) {
            super(questionId, "urn:quizup:question:alreadyApproved",
                    "Question already approved",
                    "The question " + questionId + " has already been approved");
        }
    }

    public static class QuestionAlreadyRejectedProblem extends QuestionProblem {
        public QuestionAlreadyRejectedProblem(String questionId) {
            super(questionId, "urn:quizup:question:alreadyRejected",
                    "Question already rejected",
                    "The question " + questionId + " has already been rejected");
        }
    }

    public static class NotEnoughApprovedQuestionsProblem extends QuestionProblem {
        public NotEnoughApprovedQuestionsProblem(String topicId, int current, int required) {
            super(topicId, "urn:quizup:question:notEnough",
                    "Not enough approved questions",
                    "Topic requires at least " + required + " approved questions to be published, but only " + current + " exist",
                    Map.of("current", current, "required", required));
        }
    }
}

```

### ./quizup-theme-domain/src/main/java/io/github/quizup/theme/domain/exception/TopicProblem.java
```java
package io.github.quizup.theme.domain.exception;

import io.github.quizup.common.domain.exception.BaseProblem;
import io.github.quizup.common.domain.exception.ProblemCategory;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

/**
 * Classe de base pour toutes les exceptions métier liées au domaine Topic
 */
@Getter
public abstract class TopicProblem extends BaseProblem {

    private final String topicId;

    protected TopicProblem(
            String topicId,
            String type,
            ProblemCategory category,
            String title,
            String detail,
            Map<String, Object> context) {
        super(
            type,
            category,
            title,
            detail,
            mergeContext(context, topicId)
        );
        this.topicId = topicId;
    }

    protected TopicProblem(
            String topicId,
            String type,
            String title,
            String detail,
            Map<String, Object> context) {
        this(topicId, type, ProblemCategory.BUSINESS_INVALID_COMMAND, title, detail, context);
    }

    protected TopicProblem(
            String topicId,
            String type,
            String title,
            String detail) {
        this(topicId, type, ProblemCategory.BUSINESS_INVALID_COMMAND, title, detail, null);
    }

    protected TopicProblem(
            String topicId,
            String type,
            String title) {
        this(topicId, type, title, null, null);
    }

    private static Map<String, Object> mergeContext(Map<String, Object> context, String topicId) {
        Map<String, Object> merged = new HashMap<>();
        if (context != null) {
            merged.putAll(context);
        }
        merged.put("topicId", topicId);
        return merged;
    }

}
```

### ./quizup-theme-domain/src/main/java/io/github/quizup/theme/domain/exception/TopicProblems.java
```java
package io.github.quizup.theme.domain.exception;

import io.github.quizup.common.domain.exception.ProblemCategory;
import io.github.quizup.theme.domain.model.QuestionStatus;

import java.util.Map;

/**
 * Exceptions spécifiques au domaine des thèmes, encapsulant les problèmes liés à la gestion des thèmes.
 */
public final class TopicProblems {

    private TopicProblems() {
        // Classe utilitaire
    }

    public static class TopicNotFoundProblem extends TopicProblem {
        public TopicNotFoundProblem(String topicId) {
            super(topicId, "urn:quizup:topic:notFound",
                  ProblemCategory.BUSINESS_RESOURCE_MISSING,
                  "Topic not found",
                  "The topic " + topicId + " was not found", null);
        }
    }

    public static class TopicCategoryEmptyProblem extends TopicProblem {
        public TopicCategoryEmptyProblem(String topicId) {
            super(topicId, "urn:quizup:topic:categoryEmpty", "Topic name cannot be empty");
        }
    }

    public static class TopicNameEmptyProblem extends TopicProblem {
        public TopicNameEmptyProblem(String topicId) {
            super(topicId, "urn:quizup:topic:nameEmpty", "Topic name cannot be empty");
        }
    }

    public static class CreatorIdEmptyProblem extends TopicProblem {
        public CreatorIdEmptyProblem(String topicId) {
            super(topicId, "urn:quizup:topic:creatorIdEmpty", "Creator ID cannot be empty");
        }
    }

    public static class TopicAlreadyPublishedProblem extends TopicProblem {
        public TopicAlreadyPublishedProblem(String topicId) {
            super(topicId, "urn:quizup:topic:alreadyPublished",
                    ProblemCategory.BUSINESS_INVALID_COMMAND,
                    "Topic already published",
                    "The topic " + topicId + " is already published", null);
        }
    }

    public static class TopicNotInDraftProblem extends TopicProblem {
        public TopicNotInDraftProblem(String topicId) {
            super(topicId, "urn:quizup:topic:notInDraft",
                    ProblemCategory.BUSINESS_INVALID_COMMAND,
                    "Topic not in draft status",
                    "The topic " + topicId + " must be in DRAFT status to be published", null);
        }
    }

    public static class TopicUnauthorizedAccessProblem extends TopicProblem {
        public TopicUnauthorizedAccessProblem(String topicId, String requesterId) {
            super(topicId, "urn:quizup:topic:unauthorized",
                    ProblemCategory.BUSINESS_INVALID_COMMAND,
                    "Unauthorized topic access",
                    "User " + requesterId + " is not authorized to modify topic " + topicId,
                    Map.of("requesterId", requesterId));
        }
    }

    public static class TopicFollowersCounterUnderflowProblem extends TopicProblem {
        public TopicFollowersCounterUnderflowProblem(String topicId) {
            super(topicId, "urn:quizup:topic:followersCounter:underflow",
                    ProblemCategory.BUSINESS_INVALID_COMMAND,
                    "Followers counter underflow",
                    "Followers counter cannot be negative for topic " + topicId,
                    null);
        }
    }

    public static class TopicQuestionStatusRequiredProblem extends TopicProblem {
        public TopicQuestionStatusRequiredProblem(String topicId) {
            super(topicId, "urn:quizup:topic:questionStatus:required",
                    ProblemCategory.BUSINESS_INVALID_COMMAND,
                    "Question status is required",
                    "A question status is required to update topic question counters",
                    null);
        }
    }

    public static class TopicQuestionsCounterUnderflowProblem extends TopicProblem {
        public TopicQuestionsCounterUnderflowProblem(String topicId, QuestionStatus questionStatus) {
            super(topicId, "urn:quizup:topic:questionsCounter:underflow",
                    ProblemCategory.BUSINESS_INVALID_COMMAND,
                    "Questions counter underflow",
                    "Questions counter cannot be negative for status " + questionStatus + " on topic " + topicId,
                    Map.of("questionStatus", questionStatus));
        }
    }
}
```

### ./quizup-theme-domain/src/main/java/io/github/quizup/theme/domain/model/Question.java
```java
package io.github.quizup.theme.domain.model;

import lombok.Builder;

import java.time.Instant;
import java.util.Map;

/**
 * Modèle domaine d'une question.
 */
@Builder(toBuilder = true)
public record Question(
        String questionId,
        String topicId,
        String text,
        Map<QuestionChoice, String> answers,
        QuestionChoice correctAnswer,
        QuestionStatus status,
        String creatorId,
        String updatedBy,
        Instant createdAt,
        Instant updatedAt
) {
}

```

### ./quizup-theme-domain/src/main/java/io/github/quizup/theme/domain/model/QuestionChoice.java
```java
package io.github.quizup.theme.domain.model;

public enum QuestionChoice {
    A,
    B,
    C,
    D
}
```

### ./quizup-theme-domain/src/main/java/io/github/quizup/theme/domain/model/QuestionDifficulty.java
```java
package io.github.quizup.theme.domain.model;

public enum QuestionDifficulty {
    EASY,
    MEDIUM,
    HARD,
    EXPERT
}
```

### ./quizup-theme-domain/src/main/java/io/github/quizup/theme/domain/model/QuestionLanguage.java
```java
package io.github.quizup.theme.domain.model;

public enum QuestionLanguage {
    FRENCH,
    ENGLISH,
}
```

### ./quizup-theme-domain/src/main/java/io/github/quizup/theme/domain/model/QuestionStatus.java
```java
package io.github.quizup.theme.domain.model;

public enum QuestionStatus {
    PENDING,
    APPROVED,
    REJECTED
}

```

### ./quizup-theme-domain/src/main/java/io/github/quizup/theme/domain/model/QuestionType.java
```java
package io.github.quizup.theme.domain.model;

public enum QuestionType {
    MULTIPLE_CHOICE
}
```

### ./quizup-theme-domain/src/main/java/io/github/quizup/theme/domain/model/Topic.java
```java
package io.github.quizup.theme.domain.model;

import lombok.Builder;

import java.time.Instant;
import java.util.Map;

/**
 * Modèle domaine d'un theme.
 */
@Builder(toBuilder = true)
public record Topic(
        String topicId,
        String name,
        String description,
        TopicCategory category,
        TopicStatus status,
        String creatorId,
        String updatedBy,
        Integer followersCounter,
        Map<QuestionStatus, Integer> questionsCounter,
        Instant createdAt,
        Instant updatedAt
) {
}

```

### ./quizup-theme-domain/src/main/java/io/github/quizup/theme/domain/model/TopicCategory.java
```java
package io.github.quizup.theme.domain.model;

public enum TopicCategory {
    ARTS,
    BUSINESS,
    EDUCATION,
    ENTERTAINMENT,
    FOOD_AND_DRINK,
    GAMES,
    GENERAL,
    HISTORY,
    LITERATURE,
    MOVIES,
    MUSIC,
    NATURE,
    SCIENCE,
    SPORTS,
    TELEVISION,
    TECHNOLOGY,
    WORLD
}

```

### ./quizup-theme-domain/src/main/java/io/github/quizup/theme/domain/model/TopicRules.java
```java
package io.github.quizup.theme.domain.model;

public interface TopicRules {
     int MIN_QUESTIONS_TO_PUBLISH = 7;
}
```

### ./quizup-theme-domain/src/main/java/io/github/quizup/theme/domain/model/TopicStatus.java
```java
package io.github.quizup.theme.domain.model;

public enum TopicStatus {
    DRAFT,
    PUBLISHED,
    ARCHIVED
}

```

### ./quizup-theme-domain/src/main/java/io/github/quizup/theme/domain/port/in/ApproveQuestionUseCase.java
```java
package io.github.quizup.theme.domain.port.in;

import io.github.quizup.theme.domain.command.QuestionCommand;

import java.util.concurrent.CompletableFuture;

public interface ApproveQuestionUseCase {

    CompletableFuture<String> approve(QuestionCommand.ApproveQuestionCommand command);

    default CompletableFuture<String> approve(String questionId, String requesterId) {
        return approve(
                new QuestionCommand.ApproveQuestionCommand(
                        questionId,
                        requesterId
                )
        );
    }

    default void approveAndWait(String questionId, String requesterId) {
        approve(questionId, requesterId).join();
    }
}

```

### ./quizup-theme-domain/src/main/java/io/github/quizup/theme/domain/port/in/CheckTopicUseCase.java
```java
package io.github.quizup.theme.domain.port.in;

import io.github.quizup.theme.domain.query.TopicQuery;

import java.util.concurrent.CompletableFuture;

public interface CheckTopicUseCase {

    CompletableFuture<Boolean> existsById(TopicQuery.TopicExistsByIdQuery query);

    default CompletableFuture<Boolean> existsById(String topicId) {
        return existsById(
                new TopicQuery.TopicExistsByIdQuery(
                        topicId
                )
        );
    }

    default Boolean existsByIdAndWait(String topicId) {
        return existsById(topicId).join();
    }
}

```

### ./quizup-theme-domain/src/main/java/io/github/quizup/theme/domain/port/in/CountApprovedQuestionsByTopicUseCase.java
```java
package io.github.quizup.theme.domain.port.in;

import io.github.quizup.theme.domain.query.QuestionQuery;

import java.util.concurrent.CompletableFuture;

public interface CountApprovedQuestionsByTopicUseCase {

    CompletableFuture<Integer> countApprovedQuestionsByTopic(QuestionQuery.CountApprovedQuestionsByTopicQuery query);

    default CompletableFuture<Integer> countApprovedQuestionsByTopic(String topicId){
        return countApprovedQuestionsByTopic(
                new QuestionQuery.CountApprovedQuestionsByTopicQuery(
                        topicId
                )
        );
    }
}

```

### ./quizup-theme-domain/src/main/java/io/github/quizup/theme/domain/port/in/CreateQuestionUseCase.java
```java
package io.github.quizup.theme.domain.port.in;

import io.github.quizup.theme.domain.command.QuestionCommand;
import io.github.quizup.theme.domain.model.QuestionChoice;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

public interface CreateQuestionUseCase {

    CompletableFuture<String> create(QuestionCommand.CreateQuestionCommand command);

    default CompletableFuture<String> create(String questionId,
                                           String topicId,
                                           String text,
                                           Map<QuestionChoice, String> answers,
                                           QuestionChoice correctAnswer,
                                           String creatorId) {

        return create(
                new QuestionCommand.CreateQuestionCommand(
                        questionId,
                        topicId,
                        text,
                        answers,
                        correctAnswer,
                        creatorId
                )
        );
    }

    default void createAndWait(String questionId,
                               String topicId,
                               String text,
                               Map<QuestionChoice, String> answers,
                               QuestionChoice correctAnswer,
                               String creatorId) {
        create(questionId, topicId, text, answers, correctAnswer, creatorId).join();
    }
}

```

### ./quizup-theme-domain/src/main/java/io/github/quizup/theme/domain/port/in/CreateTopicUseCase.java
```java
package io.github.quizup.theme.domain.port.in;

import io.github.quizup.theme.domain.command.TopicCommand;
import io.github.quizup.theme.domain.model.TopicCategory;

import java.util.concurrent.CompletableFuture;

public interface CreateTopicUseCase {

    CompletableFuture<String> create(TopicCommand.CreateTopicCommand command);

    default CompletableFuture<String> create(
            String topicId,
            String name,
            String description,
            TopicCategory category,
            String creatorId
    ) {
        return create(
                new TopicCommand.CreateTopicCommand(
                        topicId,
                        name,
                        description,
                        category,
                        creatorId
                )
        );
    }

    default void createAndWait(
            String topicId,
            String name,
            String description,
            TopicCategory category,
            String creatorId
    ) {
        create(topicId, name, description, category, creatorId).join();
    }
}

```

### ./quizup-theme-domain/src/main/java/io/github/quizup/theme/domain/port/in/GetQuestionUseCase.java
```java
package io.github.quizup.theme.domain.port.in;

import io.github.quizup.theme.domain.exception.QuestionProblems;
import io.github.quizup.theme.domain.model.Question;
import io.github.quizup.theme.domain.query.QuestionQuery;

import java.util.concurrent.CompletableFuture;

public interface GetQuestionUseCase {

    CompletableFuture<Question> getById(QuestionQuery.GetQuestionByIdQuery query) throws QuestionProblems.QuestionNotFoundProblem;

    default CompletableFuture<Question> getById(String questionId) throws QuestionProblems.QuestionNotFoundProblem {
        return getById(
                new QuestionQuery.GetQuestionByIdQuery(
                        questionId
                )
        );
    }
}

```

### ./quizup-theme-domain/src/main/java/io/github/quizup/theme/domain/port/in/GetRandomApprovedQuestionsUseCase.java
```java
package io.github.quizup.theme.domain.port.in;

import io.github.quizup.theme.domain.model.Question;
import io.github.quizup.theme.domain.query.QuestionQuery;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface GetRandomApprovedQuestionsUseCase {

    CompletableFuture<List<Question>> getRandomApprovedQuestions(QuestionQuery.GetRandomApprovedQuestionsQuery query);

    default CompletableFuture<List<Question>> getRandomApprovedQuestions(String topicId, int count) {
        return getRandomApprovedQuestions(
                new QuestionQuery.GetRandomApprovedQuestionsQuery(
                        topicId,
                        count
                )
        );
    }
}

```

### ./quizup-theme-domain/src/main/java/io/github/quizup/theme/domain/port/in/GetTopicUseCase.java
```java
package io.github.quizup.theme.domain.port.in;

import io.github.quizup.theme.domain.exception.TopicProblems;
import io.github.quizup.theme.domain.model.Topic;
import io.github.quizup.theme.domain.query.TopicQuery;

import java.util.concurrent.CompletableFuture;

public interface GetTopicUseCase {
    CompletableFuture<Topic> getById(TopicQuery.GetTopicByIdQuery query) throws TopicProblems.TopicNotFoundProblem;

    default CompletableFuture<Topic> getById(String topicId) throws TopicProblems.TopicNotFoundProblem {
        return getById(
                new TopicQuery.GetTopicByIdQuery(
                        topicId
                )
        );
    }
}

```

### ./quizup-theme-domain/src/main/java/io/github/quizup/theme/domain/port/in/PublishTopicUseCase.java
```java
package io.github.quizup.theme.domain.port.in;

import io.github.quizup.theme.domain.command.TopicCommand;

import java.util.concurrent.CompletableFuture;

public interface PublishTopicUseCase {
    CompletableFuture<String> publish(TopicCommand.PublishTopicCommand command);

    default CompletableFuture<String> publish(String topicId, String requesterId) {
        return publish(
                new TopicCommand.PublishTopicCommand(
                        topicId,
                        requesterId
                )
        );
    }

    default void publishAndWait(String topicId, String requesterId) {
        publish(topicId, requesterId).join();
    }
}

```

### ./quizup-theme-domain/src/main/java/io/github/quizup/theme/domain/port/in/RejectQuestionUseCase.java
```java
package io.github.quizup.theme.domain.port.in;

import io.github.quizup.theme.domain.command.QuestionCommand;

import java.util.concurrent.CompletableFuture;

public interface RejectQuestionUseCase {
    CompletableFuture<String> reject(QuestionCommand.RejectQuestionCommand command);

    default CompletableFuture<String> reject(String questionId, String requesterId, String reason) {
        return reject(
                new QuestionCommand.RejectQuestionCommand(
                        questionId,
                        requesterId,
                        reason
                )
        );
    }
}

```

### ./quizup-theme-domain/src/main/java/io/github/quizup/theme/domain/port/in/SearchQuestionUseCase.java
```java
package io.github.quizup.theme.domain.port.in;

import io.github.quizup.common.domain.model.search.*;
import io.github.quizup.theme.domain.model.Question;
import io.github.quizup.theme.domain.query.QuestionQuery;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface SearchQuestionUseCase {

    CompletableFuture<PageResult<Question>> search(QuestionQuery.QuestionSearchQuery query);

    default CompletableFuture<PageResult<Question>> search(List<FilterCriteria> filters,
                                                           List<SortCriteria> sorts,
                                                           PageCriteria page) {
        return search(
                new QuestionQuery.QuestionSearchQuery(
                        filters,
                        sorts,
                        page
                )
        );

    }
}

```

### ./quizup-theme-domain/src/main/java/io/github/quizup/theme/domain/port/in/SearchTopicUseCase.java
```java
package io.github.quizup.theme.domain.port.in;

import io.github.quizup.common.domain.model.search.*;
import io.github.quizup.theme.domain.model.Topic;
import io.github.quizup.theme.domain.query.TopicQuery;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface SearchTopicUseCase {

    CompletableFuture<PageResult<Topic>> search(TopicQuery.TopicSearchQuery query);

    default CompletableFuture<PageResult<Topic>> search(List<FilterCriteria> filters,
                                                        List<SortCriteria> sorts,
                                                        PageCriteria page) {
        return search(
                new TopicQuery.TopicSearchQuery(
                        filters,
                        sorts,
                        page
                )
        );
    }
}

```

### ./quizup-theme-domain/src/main/java/io/github/quizup/theme/domain/port/out/QuestionRepositoryPort.java
```java
package io.github.quizup.theme.domain.port.out;

import io.github.quizup.common.domain.model.search.PageResult;
import io.github.quizup.common.domain.model.search.SearchCriteria;
import io.github.quizup.theme.domain.model.Question;

import java.util.List;
import java.util.Optional;

public interface QuestionRepositoryPort {

    void save(Question question);

    Optional<Question> findById(String questionId);

    int countApprovedByTopicId(String topicId);

    List<Question> findRandomApprovedByTopicId(String topicId, int count);

    PageResult<Question> findAll(SearchCriteria searchCriteria);
}
```

### ./quizup-theme-domain/src/main/java/io/github/quizup/theme/domain/port/out/TopicRepositoryPort.java
```java
package io.github.quizup.theme.domain.port.out;

import io.github.quizup.common.domain.model.search.PageResult;
import io.github.quizup.common.domain.model.search.SearchCriteria;
import io.github.quizup.theme.domain.model.Topic;

import java.util.Optional;

public interface TopicRepositoryPort {
    void save(Topic topic);
    Optional<Topic> findById(String topicId);
    boolean existsById(String topicId);
    PageResult<Topic> findAll(SearchCriteria searchCriteria);
}
```

### ./quizup-theme-domain/src/main/java/io/github/quizup/theme/domain/query/QuestionQuery.java
```java
package io.github.quizup.theme.domain.query;

import io.github.quizup.common.domain.model.search.FilterCriteria;
import io.github.quizup.common.domain.model.search.PageCriteria;
import io.github.quizup.common.domain.model.search.SortCriteria;
import io.github.quizup.common.domain.query.SearchQuery;

import java.util.List;

public interface QuestionQuery {

    record QuestionSearchQuery(
            List<FilterCriteria> filters,
            List<SortCriteria> sorts,
            PageCriteria page
    ) implements QuestionQuery, SearchQuery {
    }

    /**
     * Query pour récupérer une question par son ID
     */
    record GetQuestionByIdQuery(
            String questionId
    ) {
    }

    /**
     * Query pour récupérer des questions aléatoires approuvées pour un duel
     */
    record GetRandomApprovedQuestionsQuery(
            String topicId,
            int count
    ) {
    }

    record CountApprovedQuestionsByTopicQuery(
            String topicId
    ) {
    }
}
```

### ./quizup-theme-domain/src/main/java/io/github/quizup/theme/domain/query/TopicQuery.java
```java
package io.github.quizup.theme.domain.query;

import io.github.quizup.common.domain.model.search.FilterCriteria;
import io.github.quizup.common.domain.model.search.PageCriteria;
import io.github.quizup.common.domain.model.search.SortCriteria;
import io.github.quizup.common.domain.query.SearchQuery;

import java.util.List;

public interface TopicQuery {

    record TopicSearchQuery(
            List<FilterCriteria> filters,
            List<SortCriteria> sorts,
            PageCriteria page
    ) implements TopicQuery, SearchQuery {

    }

    /**
     * Query pour vérifier l'existence d'un thème
     */
    record TopicExistsByIdQuery(
            String topicId
    ) {
    }

    /**
     * Query pour récupérer un thème par son ID
     */
    record GetTopicByIdQuery(
            String topicId
    ) {
    }
}
```

### ./quizup-theme-infrastructure/src/main/java/io/github/quizup/theme/application/handler/query/QuestionQueryHandler.java
```java
package io.github.quizup.theme.application.handler.query;

import io.github.quizup.common.domain.model.search.PageResult;
import io.github.quizup.theme.domain.exception.QuestionProblems;
import io.github.quizup.theme.domain.model.Question;
import io.github.quizup.theme.domain.port.out.QuestionRepositoryPort;
import io.github.quizup.theme.domain.query.QuestionQuery;
import org.axonframework.queryhandling.QueryHandler;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class QuestionQueryHandler {

    private final QuestionRepositoryPort questionRepositoryPort;

    public QuestionQueryHandler(QuestionRepositoryPort questionRepositoryPort) {
        this.questionRepositoryPort = questionRepositoryPort;
    }

    @QueryHandler
    public Question handle(QuestionQuery.GetQuestionByIdQuery query) {
        return questionRepositoryPort.findById(query.questionId())
                .orElseThrow(() -> new QuestionProblems.QuestionNotFoundProblem(query.questionId()));
    }

    @QueryHandler
    public List<Question> handle(QuestionQuery.GetRandomApprovedQuestionsQuery query) {
        return questionRepositoryPort.findRandomApprovedByTopicId(query.topicId(), query.count());
    }

    @QueryHandler
    public Integer handle(QuestionQuery.CountApprovedQuestionsByTopicQuery query) {
        return questionRepositoryPort.countApprovedByTopicId(query.topicId());
    }

    @QueryHandler
    public PageResult<Question> handle(QuestionQuery.QuestionSearchQuery query) {
        return questionRepositoryPort.findAll(query);
    }
}
```

### ./quizup-theme-infrastructure/src/main/java/io/github/quizup/theme/application/handler/query/TopicQueryHandler.java
```java
package io.github.quizup.theme.application.handler.query;

import io.github.quizup.common.domain.model.search.PageResult;
import io.github.quizup.theme.domain.exception.TopicProblems;
import io.github.quizup.theme.domain.model.Topic;
import io.github.quizup.theme.domain.port.out.TopicRepositoryPort;
import io.github.quizup.theme.domain.query.TopicQuery;
import org.axonframework.queryhandling.QueryHandler;
import org.springframework.stereotype.Component;

@Component
public class TopicQueryHandler {

    private final TopicRepositoryPort topicRepositoryPort;

    public TopicQueryHandler(TopicRepositoryPort topicRepositoryPort) {
        this.topicRepositoryPort = topicRepositoryPort;
    }

    @QueryHandler
    public PageResult<Topic> handle(TopicQuery.TopicSearchQuery query) {
        return topicRepositoryPort.findAll(query);
    }

    @QueryHandler
    public Topic handle(TopicQuery.GetTopicByIdQuery query) {
        return topicRepositoryPort.findById(query.topicId())
                .orElseThrow(() -> new TopicProblems.TopicNotFoundProblem(query.topicId()));
    }

    @QueryHandler
    public boolean handle(TopicQuery.TopicExistsByIdQuery query) {
        return topicRepositoryPort.existsById(query.topicId());
    }
}
```

### ./quizup-theme-infrastructure/src/main/java/io/github/quizup/theme/application/projection/QuestionProjection.java
```java
package io.github.quizup.theme.application.projection;

import io.github.quizup.theme.domain.event.QuestionEvent;
import io.github.quizup.theme.domain.model.Question;
import io.github.quizup.theme.domain.model.QuestionStatus;
import io.github.quizup.theme.domain.port.out.QuestionRepositoryPort;
import org.axonframework.eventhandling.EventHandler;
import org.springframework.stereotype.Component;

@Component
public class QuestionProjection {

    private final QuestionRepositoryPort questionRepositoryPort;

    public QuestionProjection(QuestionRepositoryPort questionRepositoryPort) {
        this.questionRepositoryPort = questionRepositoryPort;
    }


    @EventHandler
    public void on(QuestionEvent.QuestionCreatedEvent event) {
        Question question = new Question(
                event.questionId(),
                event.topicId(),
                event.text(),
                event.answers(),
                event.correctAnswer(),
                QuestionStatus.PENDING,
                event.creatorId(),
                event.creatorId(),
                event.createdAt(),
                event.createdAt()
        );
        questionRepositoryPort.save(question);
    }

    @EventHandler
    public void on(QuestionEvent.QuestionApprovedEvent event) {
        questionRepositoryPort.findById(event.questionId())
                .ifPresent(question -> questionRepositoryPort.save(
                        question.toBuilder()
                                .status(QuestionStatus.APPROVED)
                                .updatedBy(event.updatedBy())
                                .updatedAt(event.approvedAt())
                                .build()
                ));

    }

    @EventHandler
    public void on(QuestionEvent.QuestionRejectedEvent event) {
        questionRepositoryPort.findById(event.questionId())
                .ifPresent(question -> questionRepositoryPort.save(
                        question.toBuilder()
                                .status(QuestionStatus.REJECTED)
                                .updatedBy(event.updatedBy())
                                .updatedAt(event.rejectedAt())
                                .build()
                ));
    }
}
```

### ./quizup-theme-infrastructure/src/main/java/io/github/quizup/theme/application/projection/TopicProjection.java
```java
package io.github.quizup.theme.application.projection;

import io.github.quizup.theme.domain.event.TopicEvent;
import io.github.quizup.theme.domain.model.QuestionStatus;
import io.github.quizup.theme.domain.model.Topic;
import io.github.quizup.theme.domain.model.TopicStatus;
import io.github.quizup.theme.domain.port.out.TopicRepositoryPort;
import org.axonframework.eventhandling.EventHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumMap;
import java.util.Map;

@Component
public class TopicProjection {

    private final TopicRepositoryPort topicRepositoryPort;

    public TopicProjection(TopicRepositoryPort topicRepositoryPort) {
        this.topicRepositoryPort = topicRepositoryPort;
    }

    @EventHandler
    @Transactional
    public void on(TopicEvent.TopicCreatedEvent event) {
        Map<QuestionStatus, Integer> questionsCounters = new EnumMap<>(QuestionStatus.class);
        questionsCounters.put(QuestionStatus.PENDING, 0);
        questionsCounters.put(QuestionStatus.APPROVED, 0);
        questionsCounters.put(QuestionStatus.REJECTED, 0);

        Topic topic = new Topic(
                event.topicId(),
                event.name(),
                event.description(),
                event.category(),
                TopicStatus.DRAFT,
                event.creatorId(),
                event.creatorId(),
                0,
                questionsCounters,
                event.createdAt(),
                event.createdAt()
        );
        topicRepositoryPort.save(topic);
    }

    @EventHandler
    @Transactional
    public void on(TopicEvent.TopicPublishedEvent event) {
        topicRepositoryPort.findById(event.topicId())
                .ifPresent(topic -> topicRepositoryPort.save(
                        topic.toBuilder()
                                .status(TopicStatus.PUBLISHED)
                                .updatedBy(event.updatedBy())
                                .updatedAt(event.publishedAt())
                                .build()
                ));
    }

    @EventHandler
    @Transactional
    public void on(TopicEvent.TopicFollowersCounterIncreasedEvent event) {
        topicRepositoryPort.findById(event.topicId())
                .ifPresent(topic -> {
                    int newCount = (topic.followersCounter() != null ? topic.followersCounter() : 0) + 1;
                    topicRepositoryPort.save(
                            topic.toBuilder()
                                    .followersCounter(newCount)
                                    .updatedAt(event.updatedAt())
                                    .build()
                    );
                });
    }

    @EventHandler
    @Transactional
    public void on(TopicEvent.TopicFollowersCounterDecreasedEvent event) {
        topicRepositoryPort.findById(event.topicId())
                .ifPresent(topic -> {
                    int newCount = Math.max(0, (topic.followersCounter() != null ? topic.followersCounter() : 0) - 1);
                    topicRepositoryPort.save(
                            topic.toBuilder()
                                    .followersCounter(newCount)
                                    .updatedAt(event.updatedAt())
                                    .build()
                    );
                });
    }

    @EventHandler
    @Transactional
    public void on(TopicEvent.TopicQuestionsCounterIncreasedEvent event) {
        topicRepositoryPort.findById(event.topicId())
                .ifPresent(topic -> {
                    Map<QuestionStatus, Integer> updatedCounters = new EnumMap<>(topic.questionsCounter());
                    updatedCounters.merge(event.questionStatus(), 1, Integer::sum);
                    topicRepositoryPort.save(
                            topic.toBuilder()
                                    .questionsCounter(updatedCounters)
                                    .updatedAt(event.updatedAt())
                                    .build()
                    );
                });
    }

    @EventHandler
    @Transactional
    public void on(TopicEvent.TopicQuestionsCounterDecreasedEvent event) {
        topicRepositoryPort.findById(event.topicId())
                .ifPresent(topic -> {
                    Map<QuestionStatus, Integer> updatedCounters = new EnumMap<>(topic.questionsCounter());
                    updatedCounters.computeIfPresent(event.questionStatus(), (_, count) -> count - 1);
                    topicRepositoryPort.save(
                            topic.toBuilder()
                                    .questionsCounter(updatedCounters)
                                    .updatedAt(event.updatedAt())
                                    .build()
                    );
                });
    }
}
```

### ./quizup-theme-infrastructure/src/main/java/io/github/quizup/theme/application/projection/TopicQuestionsCounterProjection.java
```java
package io.github.quizup.theme.application.projection;

import io.github.quizup.theme.domain.command.TopicCommand;
import io.github.quizup.theme.domain.event.QuestionEvent;
import io.github.quizup.theme.domain.model.QuestionStatus;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.eventhandling.EventHandler;
import org.springframework.stereotype.Service;

@Service
public class TopicQuestionsCounterProjection {
    private final CommandGateway commandGateway;

    public TopicQuestionsCounterProjection(CommandGateway commandGateway) {
        this.commandGateway = commandGateway;
    }

    @EventHandler
    public void on(QuestionEvent.QuestionCreatedEvent event) {
        commandGateway.sendAndWait(new TopicCommand.IncreaseQuestionsCounterCommand(event.topicId(), QuestionStatus.PENDING));
    }

    @EventHandler
    public void on(QuestionEvent.QuestionApprovedEvent event) {
        commandGateway.sendAndWait(new TopicCommand.DecreaseQuestionsCounterCommand(event.topicId(), event.previousStatus()));
        commandGateway.sendAndWait(new TopicCommand.IncreaseQuestionsCounterCommand(event.topicId(), QuestionStatus.APPROVED));
    }

    @EventHandler
    public void on(QuestionEvent.QuestionRejectedEvent event) {
        commandGateway.sendAndWait(new TopicCommand.DecreaseQuestionsCounterCommand(event.topicId(), event.previousStatus()));
        commandGateway.sendAndWait(new TopicCommand.IncreaseQuestionsCounterCommand(event.topicId(), QuestionStatus.REJECTED));
    }
}

```

### ./quizup-theme-infrastructure/src/main/java/io/github/quizup/theme/application/service/QuestionCommandService.java
```java
package io.github.quizup.theme.application.service;

import io.github.quizup.theme.domain.command.QuestionCommand;
import io.github.quizup.theme.domain.port.in.ApproveQuestionUseCase;
import io.github.quizup.theme.domain.port.in.CreateQuestionUseCase;
import io.github.quizup.theme.domain.port.in.RejectQuestionUseCase;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
public class QuestionCommandService implements CreateQuestionUseCase, ApproveQuestionUseCase, RejectQuestionUseCase {

    private final CommandGateway commandGateway;

    public QuestionCommandService(CommandGateway commandGateway) {
        this.commandGateway = commandGateway;
    }


    @Override
    public CompletableFuture<String> create(QuestionCommand.CreateQuestionCommand command) {
        return commandGateway.send(command);
    }

    @Override
    public CompletableFuture<String> approve(QuestionCommand.ApproveQuestionCommand command) {
        return commandGateway.send(command);
    }
    @Override
    public CompletableFuture<String> reject(QuestionCommand.RejectQuestionCommand command) {
        return commandGateway.send(command);
    }
}

```

### ./quizup-theme-infrastructure/src/main/java/io/github/quizup/theme/application/service/QuestionQueryService.java
```java
package io.github.quizup.theme.application.service;

import io.github.quizup.common.domain.model.search.PageResult;
import io.github.quizup.common.infrastructure.axon.PageResponseTypes;
import io.github.quizup.theme.domain.exception.QuestionProblems;
import io.github.quizup.theme.domain.model.Question;
import io.github.quizup.theme.domain.port.in.CountApprovedQuestionsByTopicUseCase;
import io.github.quizup.theme.domain.port.in.GetRandomApprovedQuestionsUseCase;
import io.github.quizup.theme.domain.port.in.GetQuestionUseCase;
import io.github.quizup.theme.domain.port.in.SearchQuestionUseCase;
import io.github.quizup.theme.domain.query.QuestionQuery;
import org.axonframework.messaging.responsetypes.ResponseTypes;
import org.axonframework.queryhandling.QueryGateway;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
public class QuestionQueryService implements GetQuestionUseCase, SearchQuestionUseCase, GetRandomApprovedQuestionsUseCase, CountApprovedQuestionsByTopicUseCase {

    private final QueryGateway queryGateway;

    public QuestionQueryService(QueryGateway queryGateway) {
        this.queryGateway = queryGateway;
    }

    @Override
    public CompletableFuture<PageResult<Question>> search(QuestionQuery.QuestionSearchQuery query) {
        return queryGateway.query(query, PageResponseTypes.pageResultOf(Question.class));
    }
    @Override
    public CompletableFuture<Question> getById(QuestionQuery.GetQuestionByIdQuery query) throws QuestionProblems.QuestionNotFoundProblem {
        return queryGateway.query(query, ResponseTypes.instanceOf(Question.class));
    }

    @Override
    public CompletableFuture<List<Question>> getRandomApprovedQuestions(QuestionQuery.GetRandomApprovedQuestionsQuery query) {
        return queryGateway.query(query, ResponseTypes.multipleInstancesOf(Question.class));
    }

    @Override
    public CompletableFuture<Integer> countApprovedQuestionsByTopic(QuestionQuery.CountApprovedQuestionsByTopicQuery query) {
        return queryGateway.query(query, ResponseTypes.instanceOf(Integer.class));
    }
}

```

### ./quizup-theme-infrastructure/src/main/java/io/github/quizup/theme/application/service/TopicCommandService.java
```java
package io.github.quizup.theme.application.service;

import io.github.quizup.theme.domain.command.TopicCommand;
import io.github.quizup.theme.domain.port.in.CreateTopicUseCase;
import io.github.quizup.theme.domain.port.in.PublishTopicUseCase;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
public class TopicCommandService implements CreateTopicUseCase, PublishTopicUseCase {

    private final CommandGateway commandGateway;

    public TopicCommandService(CommandGateway commandGateway) {
        this.commandGateway = commandGateway;
    }

    @Override
    public CompletableFuture<String> create(TopicCommand.CreateTopicCommand command) {
        return commandGateway.send(command);
    }

    @Override
    public CompletableFuture<String> publish(TopicCommand.PublishTopicCommand command) {
        return commandGateway.send(command);
    }

}

```

### ./quizup-theme-infrastructure/src/main/java/io/github/quizup/theme/application/service/TopicQueryService.java
```java
package io.github.quizup.theme.application.service;

import io.github.quizup.common.domain.model.search.PageResult;
import io.github.quizup.common.infrastructure.axon.PageResponseTypes;
import io.github.quizup.theme.domain.exception.TopicProblems;
import io.github.quizup.theme.domain.model.Topic;
import io.github.quizup.theme.domain.port.in.CheckTopicUseCase;
import io.github.quizup.theme.domain.port.in.GetTopicUseCase;
import io.github.quizup.theme.domain.port.in.SearchTopicUseCase;
import io.github.quizup.theme.domain.query.TopicQuery;
import org.axonframework.messaging.responsetypes.ResponseTypes;
import org.axonframework.queryhandling.QueryGateway;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
public class TopicQueryService implements GetTopicUseCase, SearchTopicUseCase, CheckTopicUseCase {

    private final QueryGateway queryGateway;

    public TopicQueryService(QueryGateway queryGateway) {
        this.queryGateway = queryGateway;
    }

    @Override
    public CompletableFuture<Topic> getById(TopicQuery.GetTopicByIdQuery query) throws TopicProblems.TopicNotFoundProblem {
        return queryGateway.query(query, ResponseTypes.instanceOf(Topic.class));
    }

    @Override
    public CompletableFuture<Boolean> existsById(TopicQuery.TopicExistsByIdQuery query) {
        return queryGateway.query(query, ResponseTypes.instanceOf(Boolean.class));
    }

    @Override
    public CompletableFuture<PageResult<Topic>> search(TopicQuery.TopicSearchQuery query) {
        return queryGateway.query(query, PageResponseTypes.pageResultOf(Topic.class));
    }
}

```

### ./quizup-theme-infrastructure/src/main/java/io/github/quizup/theme/infrastructure/config/DataSeeder.java
```java
package io.github.quizup.theme.infrastructure.config;

import io.github.quizup.common.domain.constant.QuizUpConstants;
import io.github.quizup.theme.domain.model.QuestionChoice;
import io.github.quizup.theme.domain.model.TopicCategory;
import io.github.quizup.theme.domain.port.in.CreateQuestionUseCase;
import io.github.quizup.theme.domain.port.in.ApproveQuestionUseCase;
import io.github.quizup.theme.domain.port.in.CheckTopicUseCase;
import io.github.quizup.theme.domain.port.in.CreateTopicUseCase;
import io.github.quizup.theme.domain.port.in.PublishTopicUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

/**
 * DataSeeder - Initialise les topics et questions de test au demarrage.
 * <p>
 * Injecte uniquement des use cases (ports entrants) afin de respecter
 * l'architecture hexagonale du module.
 * <p>
 * Active uniquement si app.seed-data.enabled=true.
 */
@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DataSeeder.class);

    private final CheckTopicUseCase checkTopicUseCase;
    private final CreateTopicUseCase createTopicUseCase;
    private final CreateQuestionUseCase createQuestionUseCase;
    private final ApproveQuestionUseCase approveQuestionUseCase;
    private final PublishTopicUseCase publishTopicUseCase;

    @Value("${app.seed-data.enabled:false}")
    private boolean seedDataEnabled;

    public DataSeeder(CheckTopicUseCase checkTopicUseCase,
                      CreateTopicUseCase createTopicUseCase,
                      CreateQuestionUseCase createQuestionUseCase,
                      ApproveQuestionUseCase approveQuestionUseCase,
                      PublishTopicUseCase publishTopicUseCase) {
        this.checkTopicUseCase = checkTopicUseCase;
        this.createTopicUseCase = createTopicUseCase;
        this.createQuestionUseCase = createQuestionUseCase;
        this.approveQuestionUseCase = approveQuestionUseCase;
        this.publishTopicUseCase = publishTopicUseCase;
    }

    @Override
    public void run(String... args) {
        if (!seedDataEnabled) {
            logger.info("Data seeding is disabled (app.seed-data.enabled=false)");
            return;
        }

        logger.info("=== Starting Theme Data Seeding ===");

        try {
            seedScienceTopic();
            seedHistoryTopic();
            seedPokemonGen1Topic();
            seedPokemonGen2Topic();
            logger.info("=== Theme Data Seeding Completed Successfully ===");
        } catch (Exception e) {
            logger.error("Error during theme data seeding", e);
        }
    }

    private void seedScienceTopic() {
        String topicId = "topic-science-001";

        boolean exists = checkTopicUseCase.existsByIdAndWait(topicId);

        if (exists) {
            logger.info("Science topic already exists, skipping creation");
            return;
        }

        logger.info("Creating Science topic...");

        createTopicUseCase.createAndWait(
                topicId,
                "Sciences Générales",
                "Testez vos connaissances scientifiques !",
                TopicCategory.SCIENCE,
                QuizUpConstants.ADMIN_USER_ID
        );

        String[] questionIds = addScienceQuestions(topicId);

        for (String qId : questionIds) {
            approveQuestionUseCase.approveAndWait(qId, QuizUpConstants.ADMIN_USER_ID);
        }

        publishTopicUseCase.publishAndWait(topicId, QuizUpConstants.ADMIN_USER_ID);

        logger.info("✓ Science topic created and published: {}", topicId);
    }

    private String[] addScienceQuestions(String topicId) {
        String[] ids = new String[20];
        Object[][] questions = {
                {"Quelle est la formule chimique de l'eau ?", Map.of(QuestionChoice.A, "H2O", QuestionChoice.B, "CO2", QuestionChoice.C, "NaCl", QuestionChoice.D, "H2O2"), QuestionChoice.A},
                {"Combien y a-t-il de planètes dans le système solaire ?", Map.of(QuestionChoice.A, "7", QuestionChoice.B, "8", QuestionChoice.C, "9", QuestionChoice.D, "10"), QuestionChoice.B},
                {"Quelle est la vitesse approximative de la lumière en km/s ?", Map.of(QuestionChoice.A, "300 000", QuestionChoice.B, "150 000", QuestionChoice.C, "450 000", QuestionChoice.D, "100 000"), QuestionChoice.A},
                {"Quel est l'élément le plus abondant dans l'univers ?", Map.of(QuestionChoice.A, "Hydrogène", QuestionChoice.B, "Hélium", QuestionChoice.C, "Oxygène", QuestionChoice.D, "Carbone"), QuestionChoice.A},
                {"Quel gaz les plantes absorbent-elles pour la photosynthèse ?", Map.of(QuestionChoice.A, "CO2", QuestionChoice.B, "O2", QuestionChoice.C, "N2", QuestionChoice.D, "H2"), QuestionChoice.A},
                {"Quelle est l'unité de mesure de la fréquence ?", Map.of(QuestionChoice.A, "Hertz", QuestionChoice.B, "Volt", QuestionChoice.C, "Ampère", QuestionChoice.D, "Newton"), QuestionChoice.A},
                {"Quel est le symbole chimique du fer ?", Map.of(QuestionChoice.A, "Fe", QuestionChoice.B, "Fr", QuestionChoice.C, "Fi", QuestionChoice.D, "Fo"), QuestionChoice.A},
                {"Quel est l'organe vital responsable de la circulation sanguine ?", Map.of(QuestionChoice.A, "Poumon", QuestionChoice.B, "Cœur", QuestionChoice.C, "Cerveau", QuestionChoice.D, "Foie"), QuestionChoice.B},
                {"Combien de chromosomes un être humain possède-t-il ?", Map.of(QuestionChoice.A, "23", QuestionChoice.B, "46", QuestionChoice.C, "23 paires", QuestionChoice.D, "48"), QuestionChoice.B},
                {"Quel processus permet aux organismes de se reproduire asexuellement ?", Map.of(QuestionChoice.A, "Mitose", QuestionChoice.B, "Méiose", QuestionChoice.C, "Photosynthèse", QuestionChoice.D, "Respiration"), QuestionChoice.A},
                {"Quel est le point de fusion de la glace en Celsius ?", Map.of(QuestionChoice.A, "0°C", QuestionChoice.B, "-10°C", QuestionChoice.C, "100°C", QuestionChoice.D, "-40°C"), QuestionChoice.A},
                {"Quel gaz les animaux respirent-ils ?", Map.of(QuestionChoice.A, "Azote", QuestionChoice.B, "Oxygène", QuestionChoice.C, "Dioxyde de carbone", QuestionChoice.D, "Hydrogène"), QuestionChoice.B},
                {"Quelle est la plus grande planète du système solaire ?", Map.of(QuestionChoice.A, "Saturne", QuestionChoice.B, "Jupiter", QuestionChoice.C, "Neptune", QuestionChoice.D, "Uranus"), QuestionChoice.B},
                {"Combien de temps la lumière du Soleil met-elle pour atteindre la Terre ?", Map.of(QuestionChoice.A, "8 secondes", QuestionChoice.B, "8 minutes", QuestionChoice.C, "8 heures", QuestionChoice.D, "1 jour"), QuestionChoice.B},
                {"Quel type de liaisons chimiques existe entre les atomes d'hydrogène et d'oxygène dans l'eau ?", Map.of(QuestionChoice.A, "Liaisons ioniques", QuestionChoice.B, "Liaisons covalentes", QuestionChoice.C, "Liaisons métalliques", QuestionChoice.D, "Liaisons faibles"), QuestionChoice.B},
                {"Quel est le pH neutre ?", Map.of(QuestionChoice.A, "0", QuestionChoice.B, "7", QuestionChoice.C, "14", QuestionChoice.D, "10"), QuestionChoice.B},
                {"Quel est le plus grand océan du monde ?", Map.of(QuestionChoice.A, "Océan Atlantique", QuestionChoice.B, "Océan Indien", QuestionChoice.C, "Océan Pacifique", QuestionChoice.D, "Océan Arctique"), QuestionChoice.C},
                {"Quelle est la température de fusion du fer en Celsius ?", Map.of(QuestionChoice.A, "100°C", QuestionChoice.B, "500°C", QuestionChoice.C, "1538°C", QuestionChoice.D, "3000°C"), QuestionChoice.C},
                {"Quel gaz crée l'effet de serre ?", Map.of(QuestionChoice.A, "Dioxyde de carbone", QuestionChoice.B, "Monoxyde de carbone", QuestionChoice.C, "Méthane", QuestionChoice.D, "Dioxyde de soufre"), QuestionChoice.A},
                {"Combien de sens avons-nous ?", Map.of(QuestionChoice.A, "4", QuestionChoice.B, "5", QuestionChoice.C, "6", QuestionChoice.D, "Plus de 5"), QuestionChoice.D},
        };

        for (int i = 0; i < questions.length; i++) {
            ids[i] = UUID.randomUUID().toString();
            createQuestionUseCase.createAndWait(
                    ids[i],
                    topicId,
                    (String) questions[i][0],
                    (Map<QuestionChoice, String>) questions[i][1],
                    (QuestionChoice) questions[i][2],
                    QuizUpConstants.ADMIN_USER_ID
            );
        }
        return ids;
    }

    private void seedHistoryTopic() {
        String topicId = "topic-history-001";

        boolean exists = checkTopicUseCase.existsByIdAndWait(topicId);

        if (exists) {
            logger.info("History topic already exists, skipping creation");
            return;
        }

        logger.info("Creating History topic...");

        createTopicUseCase.createAndWait(
                topicId,
                "Histoire Mondiale",
                "Connaissez-vous l'histoire ?",
                TopicCategory.HISTORY,
                QuizUpConstants.ADMIN_USER_ID
        );

        String[] questionIds = addHistoryQuestions(topicId);

        for (String qId : questionIds) {
            approveQuestionUseCase.approveAndWait(qId, QuizUpConstants.ADMIN_USER_ID);
        }

        publishTopicUseCase.publishAndWait(topicId, QuizUpConstants.ADMIN_USER_ID);
        logger.info("✓ History topic created and published: {}", topicId);
    }

    private String[] addHistoryQuestions(String topicId) {
        String[] ids = new String[20];
        Object[][] questions = {
                {"En quelle année a débuté la Première Guerre Mondiale ?", Map.of(QuestionChoice.A, "1914", QuestionChoice.B, "1918", QuestionChoice.C, "1939", QuestionChoice.D, "1900"), QuestionChoice.A},
                {"Qui était le premier président des États-Unis ?", Map.of(QuestionChoice.A, "George Washington", QuestionChoice.B, "Abraham Lincoln", QuestionChoice.C, "Thomas Jefferson", QuestionChoice.D, "John Adams"), QuestionChoice.A},
                {"En quelle année a eu lieu la Révolution française ?", Map.of(QuestionChoice.A, "1789", QuestionChoice.B, "1776", QuestionChoice.C, "1804", QuestionChoice.D, "1815"), QuestionChoice.A},
                {"Quel empire était le plus étendu géographiquement en son apogée ?", Map.of(QuestionChoice.A, "Empire mongol", QuestionChoice.B, "Empire romain", QuestionChoice.C, "Empire britannique", QuestionChoice.D, "Empire ottoman"), QuestionChoice.A},
                {"En quelle année Napoléon a-t-il été exilé à Sainte-Hélène ?", Map.of(QuestionChoice.A, "1815", QuestionChoice.B, "1812", QuestionChoice.C, "1820", QuestionChoice.D, "1810"), QuestionChoice.A},
                {"Qui a construit les pyramides de Gizeh ?", Map.of(QuestionChoice.A, "Les Égyptiens", QuestionChoice.B, "Les Romains", QuestionChoice.C, "Les Grecs", QuestionChoice.D, "Les Phéniciens"), QuestionChoice.A},
                {"En quelle année le mur de Berlin est-il tombé ?", Map.of(QuestionChoice.A, "1989", QuestionChoice.B, "1991", QuestionChoice.C, "1985", QuestionChoice.D, "1987"), QuestionChoice.A},
                {"Qui était le leader de l'Allemagne nazie ?", Map.of(QuestionChoice.A, "Adolf Hitler", QuestionChoice.B, "Benito Mussolini", QuestionChoice.C, "Francisco Franco", QuestionChoice.D, "Joseph Staline"), QuestionChoice.A},
                {"En quelle année la Déclaration d'Indépendance américaine a-t-elle été signée ?", Map.of(QuestionChoice.A, "1776", QuestionChoice.B, "1774", QuestionChoice.C, "1778", QuestionChoice.D, "1781"), QuestionChoice.A},
                {"Quel était le nom de la capitale de l'Empire romain ?", Map.of(QuestionChoice.A, "Rome", QuestionChoice.B, "Byzance", QuestionChoice.C, "Athènes", QuestionChoice.D, "Carthage"), QuestionChoice.A},
                {"En quelle année la Révolution russe a-t-elle eu lieu ?", Map.of(QuestionChoice.A, "1905", QuestionChoice.B, "1917", QuestionChoice.C, "1922", QuestionChoice.D, "1945"), QuestionChoice.B},
                {"Qui a découvert l'Amérique en 1492 ?", Map.of(QuestionChoice.A, "Christophe Colomb", QuestionChoice.B, "Amerigo Vespucci", QuestionChoice.C, "Bartolomeu Dias", QuestionChoice.D, "Vasco de Gama"), QuestionChoice.A},
                {"En quelle année la Seconde Guerre Mondiale a-t-elle commencé ?", Map.of(QuestionChoice.A, "1937", QuestionChoice.B, "1938", QuestionChoice.C, "1939", QuestionChoice.D, "1940"), QuestionChoice.C},
                {"Quel était l'objectif principal de la Magna Carta en 1215 ?", Map.of(QuestionChoice.A, "Limiter le pouvoir du roi", QuestionChoice.B, "Augmenter le pouvoir du pape", QuestionChoice.C, "Créer une démocratie", QuestionChoice.D, "Établir un empire"), QuestionChoice.A},
                {"Qui était le premier empereur de France ?", Map.of(QuestionChoice.A, "Louis XIV", QuestionChoice.B, "Napoléon Bonaparte", QuestionChoice.C, "Charles le Grand", QuestionChoice.D, "Henri IV"), QuestionChoice.B},
                {"En quelle année Constantinople a-t-elle été conquise par les Ottomans ?", Map.of(QuestionChoice.A, "1453", QuestionChoice.B, "1389", QuestionChoice.C, "1571", QuestionChoice.D, "1683"), QuestionChoice.A},
                {"Quel événement a marqué la fin de la Préhistoire ?", Map.of(QuestionChoice.A, "L'invention de l'écriture", QuestionChoice.B, "La domestication du feu", QuestionChoice.C, "La création de l'agriculture", QuestionChoice.D, "La construction des pyramides"), QuestionChoice.A},
                {"Qui était le leader de l'Union soviétique pendant la Seconde Guerre Mondiale ?", Map.of(QuestionChoice.A, "Lénine", QuestionChoice.B, "Staline", QuestionChoice.C, "Trotski", QuestionChoice.D, "Khrouchtchev"), QuestionChoice.B},
                {"En quelle année l'Allemagne s'est-elle unifiée sous Bismarck ?", Map.of(QuestionChoice.A, "1871", QuestionChoice.B, "1848", QuestionChoice.C, "1888", QuestionChoice.D, "1864"), QuestionChoice.A},
                {"Quel était le nom du système de séparation raciale en Afrique du Sud ?", Map.of(QuestionChoice.A, "Apartheid", QuestionChoice.B, "Colonialisme", QuestionChoice.C, "Ségrégationnisme", QuestionChoice.D, "Fascisme"), QuestionChoice.A},
        };

        for (int i = 0; i < questions.length; i++) {
            ids[i] = UUID.randomUUID().toString();
            createQuestionUseCase.createAndWait(
                    ids[i],
                    topicId,
                    (String) questions[i][0],
                    (Map<QuestionChoice, String>) questions[i][1],
                    (QuestionChoice) questions[i][2],
                    QuizUpConstants.ADMIN_USER_ID
            );
        }
        return ids;
    }

    private void seedPokemonGen1Topic() {
        String topicId = "topic-pokemon-gen1-001";

        boolean exists = checkTopicUseCase.existsByIdAndWait(topicId);

        if (exists) {
            logger.info("Pokemon Gen1 topic already exists, skipping creation");
            return;
        }

        logger.info("Creating Pokemon Gen1 topic...");

        createTopicUseCase.createAndWait(
                topicId,
                "Pokémon 1G",
                "Attrapez-les tous ! Testez vos connaissances sur la 1ère génération Pokémon !",
                TopicCategory.GAMES,
                QuizUpConstants.ADMIN_USER_ID
        );

        String[] questionIds = addPokemonGen1Questions(topicId);
        for (String qId : questionIds) {
            approveQuestionUseCase.approveAndWait(qId, QuizUpConstants.ADMIN_USER_ID);
        }

        publishTopicUseCase.publishAndWait(topicId, QuizUpConstants.ADMIN_USER_ID);
        logger.info("✓ Pokemon Gen1 topic created and published: {}", topicId);
    }

    private String[] addPokemonGen1Questions(String topicId) {
        String[] ids = new String[20];
        Object[][] questions = {
                {"Combien y a-t-il de Pokémon dans la 1ère génération ?", Map.of(QuestionChoice.A, "150", QuestionChoice.B, "151", QuestionChoice.C, "152", QuestionChoice.D, "149"), QuestionChoice.B},
                {"Quel est le Pokémon de départ de type Feu dans Pokémon Rouge/Bleu ?", Map.of(QuestionChoice.A, "Salamèche", QuestionChoice.B, "Reptincel", QuestionChoice.C, "Dracaufeu", QuestionChoice.D, "Caninos"), QuestionChoice.A},
                {"Quel est le numéro de Pokédex de Mewtwo ?", Map.of(QuestionChoice.A, "149", QuestionChoice.B, "150", QuestionChoice.C, "151", QuestionChoice.D, "148"), QuestionChoice.B},
                {"Quel Pokémon légendaire est le numéro 151 ?", Map.of(QuestionChoice.A, "Mewtwo", QuestionChoice.B, "Artikodin", QuestionChoice.C, "Mew", QuestionChoice.D, "Sulfura"), QuestionChoice.C},
                {"Quelle est l'évolution finale de Carapuce ?", Map.of(QuestionChoice.A, "Carabaffe", QuestionChoice.B, "Tortank", QuestionChoice.C, "Amonistar", QuestionChoice.D, "Racaillou"), QuestionChoice.B},
                {"Quel type est Pikachu ?", Map.of(QuestionChoice.A, "Normal", QuestionChoice.B, "Feu", QuestionChoice.C, "Électrik", QuestionChoice.D, "Vol"), QuestionChoice.C},
                {"Dans quelle ville se trouve la Ligue Pokémon dans la 1ère génération ?", Map.of(QuestionChoice.A, "Parmacia", QuestionChoice.B, "Carmin-sur-Mer", QuestionChoice.C, "Plateau Indigo", QuestionChoice.D, "Lavanville"), QuestionChoice.C},
                {"Quel est le Pokémon fantôme que l'on trouve dans la Tour Pokémon ?", Map.of(QuestionChoice.A, "Spectrum", QuestionChoice.B, "Fantominus", QuestionChoice.C, "Ectoplasma", QuestionChoice.D, "Hypnomade"), QuestionChoice.B},
                {"Quelle pierre fait évoluer Pikachu en Raichu ?", Map.of(QuestionChoice.A, "Pierre Lune", QuestionChoice.B, "Pierre Feu", QuestionChoice.C, "Pierre Tonnerre", QuestionChoice.D, "Pierre Eau"), QuestionChoice.C},
                {"Quel est le premier Pokémon dans l'ordre du Pokédex ?", Map.of(QuestionChoice.A, "Pikachu", QuestionChoice.B, "Salamèche", QuestionChoice.C, "Bulbizarre", QuestionChoice.D, "Carapuce"), QuestionChoice.C},
                {"Quel outil permet de capturer les Pokémon sauvages ?", Map.of(QuestionChoice.A, "Potion", QuestionChoice.B, "Pokéball", QuestionChoice.C, "Rappel", QuestionChoice.D, "Antidote"), QuestionChoice.B},
                {"Quel est le nom du rival du joueur dans Pokémon Rouge/Bleu ?", Map.of(QuestionChoice.A, "Sacha", QuestionChoice.B, "Pierre", QuestionChoice.C, "Gary / Blue", QuestionChoice.D, "Ondine"), QuestionChoice.C},
                {"Combien y a-t-il d'arènes dans la 1ère génération ?", Map.of(QuestionChoice.A, "6", QuestionChoice.B, "7", QuestionChoice.C, "8", QuestionChoice.D, "10"), QuestionChoice.C},
                {"Quel Pokémon de type Normal/Vol est donné au joueur au début de l'aventure ?", Map.of(QuestionChoice.A, "Roucool", QuestionChoice.B, "Doduo", QuestionChoice.C, "Pikachu", QuestionChoice.D, "Rattata"), QuestionChoice.A},
                {"Quel est le type de Osselait ?", Map.of(QuestionChoice.A, "Roche", QuestionChoice.B, "Sol", QuestionChoice.C, "Spectre", QuestionChoice.D, "Normal"), QuestionChoice.D},
                {"Quelle capacité apprend Dracaufeu qui est exclusive à son espèce et de type Vol ?", Map.of(QuestionChoice.A, "Tranche-Aile", QuestionChoice.B, "Aéropique", QuestionChoice.C, "Ronflement", QuestionChoice.D, "Lance-Flammes"), QuestionChoice.B},
                {"Quel est le Pokémon fossile que l'on peut obtenir avec le Dôme Fossile ?", Map.of(QuestionChoice.A, "Amonita", QuestionChoice.B, "Kabuto", QuestionChoice.C, "Ptéra", QuestionChoice.D, "Amonistar"), QuestionChoice.B},
                {"Quel est le type de Magmar ?", Map.of(QuestionChoice.A, "Feu", QuestionChoice.B, "Feu/Vol", QuestionChoice.C, "Feu/Psy", QuestionChoice.D, "Feu/Normal"), QuestionChoice.A},
                {"Dans quelle version exclusive peut-on capturer Mewtwo ?", Map.of(QuestionChoice.A, "Uniquement Pokémon Rouge", QuestionChoice.B, "Uniquement Pokémon Bleu", QuestionChoice.C, "Les deux versions", QuestionChoice.D, "Aucune, il faut l'échanger"), QuestionChoice.C},
                {"Quel est le nom du professeur qui remet le Pokédex au joueur ?", Map.of(QuestionChoice.A, "Professeur Orme", QuestionChoice.B, "Professeur Sorbier", QuestionChoice.C, "Professeur Chen", QuestionChoice.D, "Professeur Sapin"), QuestionChoice.C},
        };

        for (int i = 0; i < questions.length; i++) {
            ids[i] = UUID.randomUUID().toString();
            createQuestionUseCase.createAndWait(
                    ids[i],
                    topicId,
                    (String) questions[i][0],
                    (Map<QuestionChoice, String>) questions[i][1],
                    (QuestionChoice) questions[i][2],
                    QuizUpConstants.ADMIN_USER_ID
            );
        }
        return ids;
    }


    private void seedPokemonGen2Topic() {
        String topicId = "topic-pokemon-gen2-001";

        boolean exists = checkTopicUseCase.existsByIdAndWait(topicId);

        if (exists) {
            logger.info("Pokemon Gen2 topic already exists, skipping creation");
            return;
        }

        logger.info("Creating Pokemon Gen2 topic...");

        createTopicUseCase.createAndWait(
                topicId,
                "Pokémon 2G",
                "Attrapez-les tous ! Testez vos connaissances sur la 2nd génération Pokémon !",
                TopicCategory.GAMES,
                QuizUpConstants.ADMIN_USER_ID
        );

        String[] questionIds = addPokemonGen2Questions(topicId);
        for (String qId : questionIds) {
            approveQuestionUseCase.approveAndWait(qId, QuizUpConstants.ADMIN_USER_ID);
        }

        publishTopicUseCase.publishAndWait(topicId, QuizUpConstants.ADMIN_USER_ID);
        logger.info("✓ Pokemon Gen2 topic created and published: {}", topicId);
    }

    private String[] addPokemonGen2Questions(String topicId) {
        String[] ids = new String[20];
        Object[][] questions = {
                {"Combien y a-t-il de nouveaux Pokémon dans la 2ème génération ?", Map.of(QuestionChoice.A, "100", QuestionChoice.B, "251", QuestionChoice.C, "151", QuestionChoice.D, "200"), QuestionChoice.A},
                {"Quel est le Pokémon de départ de type Feu dans Pokémon Or/Argent ?", Map.of(QuestionChoice.A, "Héricendre", QuestionChoice.B, "Feurisson", QuestionChoice.C, "Typhlosion", QuestionChoice.D, "Magby"), QuestionChoice.A},
                {"Quel est le Pokémon légendaire emblématique de Pokémon Or ?", Map.of(QuestionChoice.A, "Lugia", QuestionChoice.B, "Raikou", QuestionChoice.C, "Ho-Oh", QuestionChoice.D, "Suicune"), QuestionChoice.C},
                {"Quel est le Pokémon légendaire emblématique de Pokémon Argent ?", Map.of(QuestionChoice.A, "Ho-Oh", QuestionChoice.B, "Lugia", QuestionChoice.C, "Entei", QuestionChoice.D, "Célébi"), QuestionChoice.B},
                {"Dans quelle région se déroule l'aventure de la 2ème génération ?", Map.of(QuestionChoice.A, "Kanto", QuestionChoice.B, "Hoenn", QuestionChoice.C, "Johto", QuestionChoice.D, "Sinnoh"), QuestionChoice.C},
                {"Quel est le Pokémon de départ de type Eau dans Pokémon Or/Argent ?", Map.of(QuestionChoice.A, "Marill", QuestionChoice.B, "Kaiminus", QuestionChoice.C, "Crocrodil", QuestionChoice.D, "Aligatueur"), QuestionChoice.B},
                {"Quel Pokémon mythique est le numéro 251 ?", Map.of(QuestionChoice.A, "Mew", QuestionChoice.B, "Lugia", QuestionChoice.C, "Célébi", QuestionChoice.D, "Ho-Oh"), QuestionChoice.C},
                {"Quelle nouvelle mécanique a été introduite dans la 2ème génération ?", Map.of(QuestionChoice.A, "Les méga-évolutions", QuestionChoice.B, "La reproduction et les œufs", QuestionChoice.C, "Les capacités Z", QuestionChoice.D, "Les Pokémon chromatiques uniquement"), QuestionChoice.B},
                {"Quel est le nom du trio légendaire de Johto que l'on doit poursuivre ?", Map.of(QuestionChoice.A, "Raikou, Entei, Suicune", QuestionChoice.B, "Artikodin, Électhor, Sulfura", QuestionChoice.C, "Lugia, Ho-Oh, Célébi", QuestionChoice.D, "Raikou, Lugia, Entei"), QuestionChoice.A},
                {"Quelle évolution de Évoli de type Ténèbres est introduite en 2ème génération ?", Map.of(QuestionChoice.A, "Noctali", QuestionChoice.B, "Mentali", QuestionChoice.C, "Givrali", QuestionChoice.D, "Voltali"), QuestionChoice.A},
                {"Quel objet permet à Onix d'évoluer en Steelix ?", Map.of(QuestionChoice.A, "Pierre Tonnerre", QuestionChoice.B, "Manteau Métal", QuestionChoice.C, "Poing de Fer", QuestionChoice.D, "Pierre Acier"), QuestionChoice.B},
                {"Quelle ville de Johto est célèbre pour sa Tour Crampon ?", Map.of(QuestionChoice.A, "Doublonville", QuestionChoice.B, "Ecorcia", QuestionChoice.C, "Ariane", QuestionChoice.D, "Oliville"), QuestionChoice.A},
                {"Quel Pokémon bébé est le pré-évolution de Lippoutou ?", Map.of(QuestionChoice.A, "Toudoudou", QuestionChoice.B, "Mimitoss", QuestionChoice.C, "Pichu", QuestionChoice.D, "Négapi"), QuestionChoice.A},
                {"Combien y a-t-il d'arènes dans la région de Johto ?", Map.of(QuestionChoice.A, "6", QuestionChoice.B, "8", QuestionChoice.C, "10", QuestionChoice.D, "16"), QuestionChoice.B},
                {"Quel est le type d'Aligatueur, l'évolution finale de Kaiminus ?", Map.of(QuestionChoice.A, "Eau", QuestionChoice.B, "Eau/Glace", QuestionChoice.C, "Eau/Combat", QuestionChoice.D, "Eau/Ténèbres"), QuestionChoice.A},
                {"Quel appareil permet d'appeler des dresseurs pour des revanches en 2G ?", Map.of(QuestionChoice.A, "Pokénav", QuestionChoice.B, "Pokégear", QuestionChoice.C, "Pokétch", QuestionChoice.D, "Explorateur"), QuestionChoice.B},
                {"Quel est le professeur Pokémon de la 2ème génération ?", Map.of(QuestionChoice.A, "Professeur Chen", QuestionChoice.B, "Professeur Orme", QuestionChoice.C, "Professeur Sorbier", QuestionChoice.D, "Professeur Cognac"), QuestionChoice.B},
                {"Quelle région peut-on visiter après avoir battu la Ligue Pokémon en 2G ?", Map.of(QuestionChoice.A, "Johto", QuestionChoice.B, "Hoenn", QuestionChoice.C, "Kanto", QuestionChoice.D, "Sinnoh"), QuestionChoice.C},
                {"Quel objet fait évoluer Porygon en Porygon2 ?", Map.of(QuestionChoice.A, "Câble Liaison", QuestionChoice.B, "Disque Amélio", QuestionChoice.C, "Pièce Ronde", QuestionChoice.D, "Manteau Métal"), QuestionChoice.B},
                {"Quel est le nom du rival dans Pokémon Or/Argent ?", Map.of(QuestionChoice.A, "Pierre", QuestionChoice.B, "Silver", QuestionChoice.C, "Gold", QuestionChoice.D, "Kris"), QuestionChoice.B},
        };

        for (int i = 0; i < questions.length; i++) {
            ids[i] = UUID.randomUUID().toString();
            createQuestionUseCase.createAndWait(
                    ids[i],
                    topicId,
                    (String) questions[i][0],
                    (Map<QuestionChoice, String>) questions[i][1],
                    (QuestionChoice) questions[i][2],
                    QuizUpConstants.ADMIN_USER_ID
            );
        }
        return ids;
    }
}
```

### ./quizup-theme-infrastructure/src/main/java/io/github/quizup/theme/infrastructure/in/api/mapper/QuestionResponseMapper.java
```java
package io.github.quizup.theme.infrastructure.in.api.mapper;

import io.github.quizup.common.domain.model.search.PageResult;
import io.github.quizup.common.infrastructure.in.api.response.PageResponse;
import io.github.quizup.common.infrastructure.mapper.SearchResponseMapper;
import io.github.quizup.theme.domain.model.Question;
import io.github.quizup.theme.infrastructure.in.api.response.QuestionResponse;

import java.util.List;

public final class QuestionResponseMapper {

    private QuestionResponseMapper() {
    }

    public static QuestionResponse toResponse(Question question) {
        return new QuestionResponse(
                question.questionId(),
                question.topicId(),
                question.text(),
                question.answers(),
                question.correctAnswer(),
                question.status(),
                question.creatorId(),
                question.updatedBy(),
                question.createdAt(),
                question.updatedAt()
        );
    }

    public static List<QuestionResponse> toResponse(List<Question> questions) {
        return questions.stream().map(QuestionResponseMapper::toResponse).toList();
    }

    public static PageResponse<QuestionResponse> toResponse(PageResult<Question> pageResult) {
        return SearchResponseMapper.toSearchResponse(pageResult, QuestionResponseMapper::toResponse);
    }
}

```

### ./quizup-theme-infrastructure/src/main/java/io/github/quizup/theme/infrastructure/in/api/mapper/TopicResponseMapper.java
```java
package io.github.quizup.theme.infrastructure.in.api.mapper;

import io.github.quizup.common.domain.model.search.PageResult;
import io.github.quizup.common.infrastructure.in.api.response.PageResponse;
import io.github.quizup.common.infrastructure.mapper.SearchResponseMapper;
import io.github.quizup.theme.domain.model.Topic;
import io.github.quizup.theme.infrastructure.in.api.response.TopicResponse;

import java.util.List;

public final class TopicResponseMapper {

    private TopicResponseMapper() {
    }

    public static TopicResponse toResponse(Topic topic) {
        return new TopicResponse(
                topic.topicId(),
                topic.name(),
                topic.description(),
                topic.category(),
                topic.status(),
                topic.creatorId(),
                topic.updatedBy(),
                topic.followersCounter(),
                topic.questionsCounter(),
                topic.createdAt(),
                topic.updatedAt()
        );
    }

    public static List<TopicResponse> toResponse(List<Topic> topics) {
        return topics.stream().map(TopicResponseMapper::toResponse).toList();
    }

    public static PageResponse<TopicResponse> toResponse(PageResult<Topic> pageResult) {
        return SearchResponseMapper.toSearchResponse(pageResult, TopicResponseMapper::toResponse);
    }
}

```

### ./quizup-theme-infrastructure/src/main/java/io/github/quizup/theme/infrastructure/in/api/QuestionController.java
```java
package io.github.quizup.theme.infrastructure.in.api;

import io.github.quizup.common.domain.model.search.SearchCriteria;
import io.github.quizup.common.infrastructure.in.api.ResponseEntityBuilder;
import io.github.quizup.common.infrastructure.in.api.request.SearchRequest;
import io.github.quizup.common.infrastructure.in.api.response.IdResponse;
import io.github.quizup.common.infrastructure.in.api.response.PageResponse;
import io.github.quizup.common.infrastructure.mapper.SearchRequestMapper;
import io.github.quizup.microservice.infrastructure.security.SecurityHelper;
import io.github.quizup.theme.domain.port.in.*;
import io.github.quizup.theme.infrastructure.in.api.mapper.QuestionResponseMapper;
import io.github.quizup.theme.infrastructure.in.api.request.QuestionRequest;
import io.github.quizup.theme.infrastructure.in.api.response.QuestionResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * QuestionController - API REST pour la gestion des questions d'un thème
 */
@RestController
@RequestMapping(QuestionController.QUESTIONS_ENDPOINT)
public class QuestionController {

    public static final String QUESTIONS_ENDPOINT = "/api/questions";

    private final GetQuestionUseCase getQuestionUseCase;
    private final SearchQuestionUseCase searchQuestionUseCase;
    private final CreateQuestionUseCase createQuestionUseCase;
    private final ApproveQuestionUseCase approveQuestionUseCase;
    private final RejectQuestionUseCase rejectQuestionUseCase;

    public QuestionController(SearchQuestionUseCase searchQuestionUseCase,
                              CreateQuestionUseCase createQuestionUseCase,
                              ApproveQuestionUseCase approveQuestionUseCase,
                              RejectQuestionUseCase rejectQuestionUseCase,
                              GetQuestionUseCase getQuestionUseCase) {
        this.searchQuestionUseCase = searchQuestionUseCase;
        this.createQuestionUseCase = createQuestionUseCase;
        this.approveQuestionUseCase = approveQuestionUseCase;
        this.rejectQuestionUseCase = rejectQuestionUseCase;
        this.getQuestionUseCase = getQuestionUseCase;
    }


    /**
     * Search topics with pagination and sorting
     */
    @PostMapping("/search")
    public CompletableFuture<ResponseEntity<PageResponse<QuestionResponse>>> search(
            @RequestBody SearchRequest searchRequest
    ) {
        SearchCriteria searchCriteria = SearchRequestMapper.toSearchCriteria(searchRequest);
        return searchQuestionUseCase.search(
                        searchCriteria.filters(),
                        searchCriteria.sorts(),
                        searchCriteria.page()
                )
                .thenApply(QuestionResponseMapper::toResponse)
                .thenApply(ResponseEntity::ok);
    }

    @PostMapping
    public CompletableFuture<ResponseEntity<IdResponse>> createQuestion(
            @RequestBody @Valid QuestionRequest.CreateQuestionRequest request
    ) {
        String questionId = UUID.randomUUID().toString();
        String creatorId = SecurityHelper.getUserId();
        return createQuestionUseCase
                .create(
                        questionId,
                        request.topicId(),
                        request.text(),
                        request.answers(),
                        request.correctAnswer(),
                        creatorId
                )
                .thenApply(_ -> ResponseEntityBuilder.creation(QUESTIONS_ENDPOINT,questionId));
    }

    @GetMapping("/{questionId}")
    public CompletableFuture<ResponseEntity<QuestionResponse>> getQuestionById(
            @PathVariable String questionId
    ) {
        return getQuestionUseCase.getById(questionId)
                .thenApply(QuestionResponseMapper::toResponse)
                .thenApply(ResponseEntity::ok);
    }

    @PostMapping("/{questionId}/approve")
    public CompletableFuture<ResponseEntity<IdResponse>> approveQuestion(
            @PathVariable String questionId
    ) {
        String requesterId = SecurityHelper.getUserId();
        return approveQuestionUseCase.approve(questionId, requesterId)
                .thenApply(ResponseEntityBuilder::ok);
    }

    @PostMapping("/{questionId}/reject")
    public CompletableFuture<ResponseEntity<IdResponse>> rejectQuestion(
            @PathVariable String questionId,
            @RequestBody @Valid QuestionRequest.RejectQuestionRequest request
    ) {
        String requesterId = SecurityHelper.getUserId();
        return rejectQuestionUseCase.reject(questionId, requesterId, request.reason())
                .thenApply(ResponseEntityBuilder::ok);
    }
}
```

### ./quizup-theme-infrastructure/src/main/java/io/github/quizup/theme/infrastructure/in/api/request/QuestionRequest.java
```java
package io.github.quizup.theme.infrastructure.in.api.request;

import io.github.quizup.theme.domain.model.QuestionChoice;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Map;

public interface QuestionRequest {
    /**
     * Requête de rejet d'une question
     */
    record RejectQuestionRequest(
            @NotBlank(message = "La raison du rejet est obligatoire")
            String reason
    ) {
    }

    /**
     * Requête d'ajout d'une question à un thème
     */
    record CreateQuestionRequest(
            @NotBlank(message = "Le topic relatif à la question est obligatoire")
            String topicId,

            @NotBlank(message = "Le texte de la question est obligatoire")
            @Size(max = 135, message = "La question ne peut pas dépasser 135 caractères")
            String text,

            @NotNull(message = "Les réponses sont obligatoires")
            Map<QuestionChoice, @Size(max = 30, message = "Une réponse ne peut pas dépasser 30 caractères") String> answers,

            @NotNull(message = "La bonne réponse est obligatoire")
            QuestionChoice correctAnswer
    ) {
    }
}
```

### ./quizup-theme-infrastructure/src/main/java/io/github/quizup/theme/infrastructure/in/api/request/TopicRequest.java
```java
package io.github.quizup.theme.infrastructure.in.api.request;

import io.github.quizup.theme.domain.model.TopicCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public interface TopicRequest {

    /**
     * Requête de création d'un thème
     */
    record CreateTopicRequest(
            @NotBlank(message = "Le nom du thème est obligatoire")
            @Size(max = 25, message = "Le nom du thème ne peut pas dépasser 25 caractères")
            String name,

            @Size(max = 500, message = "La description ne peut pas dépasser 500 caractères")
            String description,

            @NotNull(message = "La catégorie est obligatoire")
            TopicCategory category
    ) {
    }
}
```

### ./quizup-theme-infrastructure/src/main/java/io/github/quizup/theme/infrastructure/in/api/response/QuestionResponse.java
```java
package io.github.quizup.theme.infrastructure.in.api.response;

import io.github.quizup.theme.domain.model.QuestionChoice;
import io.github.quizup.theme.domain.model.QuestionStatus;

import java.io.Serializable;
import java.time.Instant;
import java.util.Map;

/**
 * DTO de réponse pour une question
 */
public record QuestionResponse(
        String questionId,
        String topicId,
        String text,
        Map<QuestionChoice, String> answers,
        QuestionChoice correctAnswer,
        QuestionStatus status,
        String creatorId,
        String updatedBy,
        Instant createdAt,
        Instant updatedAt
) implements Serializable {
}

```

### ./quizup-theme-infrastructure/src/main/java/io/github/quizup/theme/infrastructure/in/api/response/TopicResponse.java
```java
package io.github.quizup.theme.infrastructure.in.api.response;

import io.github.quizup.theme.domain.model.QuestionStatus;
import io.github.quizup.theme.domain.model.TopicCategory;
import io.github.quizup.theme.domain.model.TopicStatus;

import java.io.Serializable;
import java.time.Instant;
import java.util.Map;

/**
 * DTO de réponse pour un thème
 */
public record TopicResponse(
        String topicId,
        String name,
        String description,
        TopicCategory category,
        TopicStatus status,
        String creatorId,
        String updatedBy,
        Integer followersCounter,
        Map<QuestionStatus, Integer> questionsCounter,
        Instant createdAt,
        Instant updatedAt
) implements Serializable {
}

```

### ./quizup-theme-infrastructure/src/main/java/io/github/quizup/theme/infrastructure/in/api/TopicController.java
```java
package io.github.quizup.theme.infrastructure.in.api;

import io.github.quizup.common.domain.model.search.SearchCriteria;
import io.github.quizup.common.infrastructure.in.api.ResponseEntityBuilder;
import io.github.quizup.common.infrastructure.in.api.request.SearchRequest;
import io.github.quizup.common.infrastructure.in.api.response.IdResponse;
import io.github.quizup.common.infrastructure.in.api.response.PageResponse;
import io.github.quizup.common.infrastructure.mapper.SearchRequestMapper;
import io.github.quizup.microservice.infrastructure.security.SecurityHelper;
import io.github.quizup.theme.domain.port.in.CreateTopicUseCase;
import io.github.quizup.theme.domain.port.in.GetTopicUseCase;
import io.github.quizup.theme.domain.port.in.PublishTopicUseCase;
import io.github.quizup.theme.domain.port.in.SearchTopicUseCase;
import io.github.quizup.theme.infrastructure.in.api.mapper.TopicResponseMapper;
import io.github.quizup.theme.infrastructure.in.api.request.TopicRequest;
import io.github.quizup.theme.infrastructure.in.api.response.TopicResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * TopicController - API REST pour la gestion des thèmes
 */
@RestController
@RequestMapping(TopicController.ENDPOINT)
public class TopicController {

    public static final String ENDPOINT = "/api/topics";

    private final GetTopicUseCase getTopicUseCase;
    private final CreateTopicUseCase createTopicUseCase;
    private final PublishTopicUseCase publishTopicUseCase;
    private final SearchTopicUseCase searchTopicUseCase;

    public TopicController(CreateTopicUseCase createTopicUseCase,
                           PublishTopicUseCase publishTopicUseCase,
                           GetTopicUseCase getTopicUseCase,
                           SearchTopicUseCase searchTopicUseCase) {
        this.createTopicUseCase = createTopicUseCase;
        this.publishTopicUseCase = publishTopicUseCase;
        this.getTopicUseCase = getTopicUseCase;
        this.searchTopicUseCase = searchTopicUseCase;
    }

    /**
     * Search topics with pagination and sorting
     */
    @PostMapping("/search")
    public CompletableFuture<ResponseEntity<PageResponse<TopicResponse>>> search(
            @RequestBody SearchRequest searchRequest
    ) {
        SearchCriteria searchCriteria = SearchRequestMapper.toSearchCriteria(searchRequest);
        return searchTopicUseCase.search(
                        searchCriteria.filters(),
                        searchCriteria.sorts(),
                        searchCriteria.page()
                )
                .thenApply(TopicResponseMapper::toResponse)
                .thenApply(ResponseEntity::ok);
    }

    /**
     * Créer un nouveau thème
     */
    @PostMapping
    public CompletableFuture<ResponseEntity<IdResponse>> createTopic(
            @RequestBody @Valid TopicRequest.CreateTopicRequest request
    ) {
        String topicId = UUID.randomUUID().toString();
        String creatorId = SecurityHelper.getUserId();
        return createTopicUseCase.create(
                        topicId,
                        request.name(),
                        request.description(),
                        request.category(),
                        creatorId
                )
                .thenApply(_ -> ResponseEntityBuilder.creation(ENDPOINT, topicId));
    }

    /**
     * Publier un thème (passage DRAFT -> PUBLISHED)
     */
    @PostMapping("/{topicId}/publish")
    public CompletableFuture<ResponseEntity<IdResponse>> publishTopic(
            @PathVariable String topicId
    ) {
        String requesterId = SecurityHelper.getUserId();
        return publishTopicUseCase.publish(topicId, requesterId)
                .thenApply(ResponseEntityBuilder::ok);
    }

    /**
     * Récupérer un thème par son ID
     */
    @GetMapping("/{topicId}")
    public CompletableFuture<ResponseEntity<TopicResponse>> getTopicById(
            @PathVariable String topicId
    ) {
        return getTopicUseCase.getById(topicId)
                .thenApply(TopicResponseMapper::toResponse)
                .thenApply(ResponseEntity::ok);
    }
}

```

### ./quizup-theme-infrastructure/src/main/java/io/github/quizup/theme/infrastructure/out/persistence/adapter/QuestionRepositoryAdapter.java
```java
package io.github.quizup.theme.infrastructure.out.persistence.adapter;

import io.github.quizup.common.domain.model.search.PageResult;
import io.github.quizup.common.domain.model.search.SearchCriteria;
import io.github.quizup.common.infrastructure.adapter.AnnotationSearchableEntity;
import io.github.quizup.common.infrastructure.adapter.JpaSearchAdapter;
import io.github.quizup.theme.domain.model.Question;
import io.github.quizup.theme.domain.port.out.QuestionRepositoryPort;
import io.github.quizup.theme.infrastructure.out.persistence.entity.QuestionEntity;
import io.github.quizup.theme.infrastructure.out.persistence.mapper.QuestionEntityMapper;
import io.github.quizup.theme.infrastructure.out.persistence.repository.QuestionJpaRepository;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Component
public class QuestionRepositoryAdapter implements QuestionRepositoryPort {

    private final QuestionJpaRepository questionJpaRepository;

    private final JpaSearchAdapter<QuestionEntity> questionJpaSearchAdapter;

    public QuestionRepositoryAdapter(QuestionJpaRepository questionJpaRepository) {
        this.questionJpaRepository = questionJpaRepository;
        this.questionJpaSearchAdapter = new JpaSearchAdapter<>(questionJpaRepository, new AnnotationSearchableEntity(QuestionEntity.class));
    }

    @Override
    @Transactional
    public void save(Question question) {
        questionJpaRepository.save(QuestionEntityMapper.toEntity(question));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Question> findById(String questionId) {
        return questionJpaRepository.findById(questionId).map(QuestionEntityMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public int countApprovedByTopicId(String topicId) {
        return questionJpaRepository.countApprovedByTopicId(topicId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Question> findRandomApprovedByTopicId(String topicId, int count) {
        return questionJpaRepository.findRandomApprovedByTopicId(topicId, count)
                .stream()
                .map(QuestionEntityMapper::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<Question> findAll(SearchCriteria searchCriteria) {
        return questionJpaSearchAdapter.findAll(searchCriteria, QuestionEntityMapper::toDomain);
    }
}

```

### ./quizup-theme-infrastructure/src/main/java/io/github/quizup/theme/infrastructure/out/persistence/adapter/TopicRepositoryAdapter.java
```java
package io.github.quizup.theme.infrastructure.out.persistence.adapter;

import io.github.quizup.common.domain.model.search.PageResult;
import io.github.quizup.common.domain.model.search.SearchCriteria;
import io.github.quizup.common.infrastructure.adapter.AnnotationSearchableEntity;
import io.github.quizup.common.infrastructure.adapter.JpaSearchAdapter;
import io.github.quizup.theme.domain.model.Topic;
import io.github.quizup.theme.domain.port.out.TopicRepositoryPort;
import io.github.quizup.theme.infrastructure.out.persistence.entity.TopicEntity;
import io.github.quizup.theme.infrastructure.out.persistence.mapper.TopicEntityMapper;
import io.github.quizup.theme.infrastructure.out.persistence.repository.TopicJpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Component
public class TopicRepositoryAdapter implements TopicRepositoryPort {

    private final TopicJpaRepository topicJpaRepository;
    private final JpaSearchAdapter<TopicEntity> topicJpaSearchAdapter;

    public TopicRepositoryAdapter(TopicJpaRepository topicJpaRepository) {
        this.topicJpaRepository = topicJpaRepository;
        this.topicJpaSearchAdapter = new JpaSearchAdapter<>(topicJpaRepository, new AnnotationSearchableEntity(TopicEntity.class));
    }

    @Override
    @Transactional
    public void save(Topic topic) {
        topicJpaRepository.save(TopicEntityMapper.toEntity(topic));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Topic> findById(String topicId) {
        return topicJpaRepository.findById(topicId)
                .map(TopicEntityMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsById(String topicId) {
        return topicJpaRepository.existsById(topicId);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<Topic> findAll(SearchCriteria searchCriteria) {
        return topicJpaSearchAdapter.findAll(searchCriteria)
                .map(TopicEntityMapper::toDomain);
    }
}

```

### ./quizup-theme-infrastructure/src/main/java/io/github/quizup/theme/infrastructure/out/persistence/entity/QuestionEntity.java
```java
package io.github.quizup.theme.infrastructure.out.persistence.entity;

import io.github.quizup.common.domain.model.search.FieldType;
import io.github.quizup.common.domain.model.search.Searchable;
import io.github.quizup.theme.domain.model.QuestionChoice;
import io.github.quizup.theme.domain.model.QuestionStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * QuestionEntity - Entité JPA pour la projection read-only des questions
 * Mise à jour via les Event Handlers (projection)
 */
@Setter
@Getter
@Entity
@Table(name = "question_entry", indexes = {
        @Index(name = "idx_question_entry_topic", columnList = "topic_id"),
        @Index(name = "idx_question_entry_status", columnList = "status"),
        @Index(name = "idx_question_entry_creator", columnList = "creator_id")
})
public class QuestionEntity {

    @Id
    @Searchable(type = FieldType.STRING)
    @Column(name = "question_id", length = 255, nullable = false)
    private String questionId;

    @Searchable(type = FieldType.STRING)
    @Column(name = "topic_id", length = 255, nullable = false)
    private String topicId;

    @Column(name = "text", length = 255, nullable = false)
    private String text;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "question_answer_entry", joinColumns = @JoinColumn(name = "question_id"))
    @MapKeyEnumerated(EnumType.STRING)
    @MapKeyColumn(name = "choice")
    @Column(name = "answer_text", length = 255, nullable = false)
    private Map<QuestionChoice, String> answers = new HashMap<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "correct_answer", nullable = false)
    private QuestionChoice correctAnswer;

    @Searchable(type = FieldType.STRING)
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private QuestionStatus status;

    @Searchable(type = FieldType.STRING)
    @Column(name = "creator_id", length = 255, nullable = false)
    private String creatorId;

    @Searchable(type = FieldType.DATE)
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Searchable(type = FieldType.STRING)
    @Column(name = "updated_by", length = 255)
    private String updatedBy;

    @Searchable(type = FieldType.DATE)
    @Column(name = "updated_at")
    private Instant updatedAt;

}

```

### ./quizup-theme-infrastructure/src/main/java/io/github/quizup/theme/infrastructure/out/persistence/entity/TopicEntity.java
```java
package io.github.quizup.theme.infrastructure.out.persistence.entity;

import io.github.quizup.common.domain.model.search.FieldType;
import io.github.quizup.common.domain.model.search.Searchable;
import io.github.quizup.theme.domain.model.QuestionStatus;
import io.github.quizup.theme.domain.model.TopicCategory;
import io.github.quizup.theme.domain.model.TopicStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * TopicEntity - Entité JPA pour la projection read-only du thème
 * Mise à jour via les Event Handlers (projection)
 */
@Setter
@Getter
@Entity
@Table(name = "topic_entry", indexes = {
        @Index(name = "idx_topic_entry_creator", columnList = "creator_id"),
        @Index(name = "idx_topic_entry_status", columnList = "status"),
        @Index(name = "idx_topic_entry_category", columnList = "category")
})
public class TopicEntity {

    @Id
    @Searchable(type = FieldType.STRING)
    @Column(name = "topic_id", length = 255, nullable = false)
    private String topicId;

    @Searchable(type = FieldType.STRING)
    @Column(name = "name", length = 25, nullable = false)
    private String name;

    @Searchable(type = FieldType.STRING)
    @Column(name = "description", length = 500)
    private String description;

    @Searchable(type = FieldType.STRING)
    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false)
    private TopicCategory category;

    @Searchable(type = FieldType.STRING)
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private TopicStatus status;

    @Searchable(type = FieldType.STRING)
    @Column(name = "creator_id", length = 255, nullable = false)
    private String creatorId;

    @Searchable(type = FieldType.NUMBER)
    @Column(name = "followers_counter")
    private Integer followersCounter;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "topic_questions_counter", joinColumns = @JoinColumn(name = "topic_id"))
    @MapKeyEnumerated(EnumType.STRING)
    @MapKeyColumn(name = "question_status")
    @Column(name = "counter", nullable = false)
    private Map<QuestionStatus, Integer> questionsCounter = new HashMap<>();

    @Searchable(type = FieldType.DATE)
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Searchable(type = FieldType.STRING)
    @Column(name = "updated_by", length = 255)
    private String updatedBy;

    @Searchable(type = FieldType.DATE)
    @Column(name = "updated_at")
    private Instant updatedAt;
}

```

### ./quizup-theme-infrastructure/src/main/java/io/github/quizup/theme/infrastructure/out/persistence/mapper/QuestionEntityMapper.java
```java
package io.github.quizup.theme.infrastructure.out.persistence.mapper;

import io.github.quizup.theme.domain.model.Question;
import io.github.quizup.theme.infrastructure.out.persistence.entity.QuestionEntity;

public final class QuestionEntityMapper {

    private QuestionEntityMapper() {
    }

    public static Question toDomain(QuestionEntity entity) {
        return new Question(
                entity.getQuestionId(),
                entity.getTopicId(),
                entity.getText(),
                entity.getAnswers(),
                entity.getCorrectAnswer(),
                entity.getStatus(),
                entity.getCreatorId(),
                entity.getUpdatedBy(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public static QuestionEntity toEntity(Question question) {
        QuestionEntity questionEntity = new QuestionEntity();
        questionEntity.setQuestionId(question.questionId());
        questionEntity.setTopicId(question.topicId());
        questionEntity.setText(question.text());
        questionEntity.setAnswers(question.answers());
        questionEntity.setCorrectAnswer(question.correctAnswer());
        questionEntity.setStatus(question.status());
        questionEntity.setCreatorId(question.creatorId());
        questionEntity.setUpdatedBy(question.updatedBy());
        questionEntity.setCreatedAt(question.createdAt());
        questionEntity.setUpdatedAt(question.updatedAt());
        return questionEntity;
    }
}

```

### ./quizup-theme-infrastructure/src/main/java/io/github/quizup/theme/infrastructure/out/persistence/mapper/TopicEntityMapper.java
```java
package io.github.quizup.theme.infrastructure.out.persistence.mapper;

import io.github.quizup.theme.domain.model.Topic;
import io.github.quizup.theme.infrastructure.out.persistence.entity.TopicEntity;

public final class TopicEntityMapper {

    private TopicEntityMapper() {
    }

    public static Topic toDomain(TopicEntity entity) {
        return new Topic(
                entity.getTopicId(),
                entity.getName(),
                entity.getDescription(),
                entity.getCategory(),
                entity.getStatus(),
                entity.getCreatorId(),
                entity.getUpdatedBy(),
                entity.getFollowersCounter(),
                entity.getQuestionsCounter(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public static TopicEntity toEntity(Topic topic) {
        TopicEntity topicEntity = new TopicEntity();
        topicEntity.setTopicId(topic.topicId());
        topicEntity.setName(topic.name());
        topicEntity.setDescription(topic.description());
        topicEntity.setCategory(topic.category());
        topicEntity.setStatus(topic.status());
        topicEntity.setCreatorId(topic.creatorId());
        topicEntity.setUpdatedBy(topic.updatedBy());
        topicEntity.setFollowersCounter(topic.followersCounter());
        topicEntity.setQuestionsCounter(topic.questionsCounter());
        topicEntity.setCreatedAt(topic.createdAt());
        topicEntity.setUpdatedAt(topic.updatedAt());
        return topicEntity;
    }
}

```

### ./quizup-theme-infrastructure/src/main/java/io/github/quizup/theme/infrastructure/out/persistence/repository/QuestionJpaRepository.java
```java
package io.github.quizup.theme.infrastructure.out.persistence.repository;

import io.github.quizup.theme.infrastructure.out.persistence.entity.QuestionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository JPA pour les projections de questions
 */
@Repository
public interface QuestionJpaRepository extends JpaRepository<QuestionEntity, String>, JpaSpecificationExecutor<QuestionEntity> {

    @Query("SELECT COUNT(q) FROM QuestionEntity q WHERE q.topicId = :topicId AND q.status = io.github.quizup.theme.domain.model.QuestionStatus.APPROVED")
    int countApprovedByTopicId(@Param("topicId") String topicId);

    @Query(value = "SELECT * FROM question_entry WHERE topic_id = :topicId AND status = 'APPROVED' ORDER BY RANDOM() LIMIT :count", nativeQuery = true)
    List<QuestionEntity> findRandomApprovedByTopicId(@Param("topicId") String topicId, @Param("count") int count);
}

```

### ./quizup-theme-infrastructure/src/main/java/io/github/quizup/theme/infrastructure/out/persistence/repository/TopicJpaRepository.java
```java
package io.github.quizup.theme.infrastructure.out.persistence.repository;

import io.github.quizup.theme.infrastructure.out.persistence.entity.TopicEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

/**
 * Repository JPA pour les projections de thèmes
 */
@Repository
public interface TopicJpaRepository extends JpaRepository<TopicEntity, String>, JpaSpecificationExecutor<TopicEntity> {

}

```

### ./quizup-theme-infrastructure/src/main/java/io/github/quizup/theme/ThemeServiceApplication.java
```java
package io.github.quizup.theme;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ThemeServiceApplication {

    static void main(String[] args) {
        SpringApplication.run(ThemeServiceApplication.class, args);
    }
}
```

