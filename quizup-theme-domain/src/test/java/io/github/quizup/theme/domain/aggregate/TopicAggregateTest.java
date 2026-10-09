package io.github.quizup.theme.domain.aggregate;

import io.github.quizup.axon.test.QuizUpAxonMatchers;
import io.github.quizup.microservice.core.domain.model.i18n.Language;
import io.github.quizup.theme.domain.command.TopicCommand;
import io.github.quizup.theme.domain.event.TopicEvent;
import io.github.quizup.theme.domain.exception.QuestionProblems;
import io.github.quizup.theme.domain.exception.TopicProblems;
import io.github.quizup.theme.domain.model.TopicCategory;
import io.github.quizup.theme.domain.port.out.QuestionRepositoryPort;
import org.axonframework.test.aggregate.AggregateTestFixture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test Axon in-memory de l'agrégat {@link TopicAggregate} : création, publication et mises à
 * jour champ par champ (propriétaire uniquement).
 */
class TopicAggregateTest {

    private static final Instant NOW = Instant.parse("2026-09-20T10:00:00Z");
    private static final String TOPIC_ID = "t-1";
    private static final String OWNER = "creator-1";

    private final AggregateTestFixture<TopicAggregate> fixture =
            new AggregateTestFixture<>(TopicAggregate.class);
    private final QuestionRepositoryPort questionRepository = mock(QuestionRepositoryPort.class);

    @BeforeEach
    void setUp() {
        fixture.registerInjectableResource(questionRepository);
    }

    @Test
    void createTopic_appliesTopicCreatedEvent() {
        fixture.givenNoPriorActivity()
                .when(createCommand("Cinéma", "Tout le cinéma"))
                .expectEventsMatching(QuizUpAxonMatchers.singlePayloadMatching(
                        TopicEvent.TopicCreatedEvent.class,
                        e -> TOPIC_ID.equals(((TopicEvent.TopicCreatedEvent) e).topicId())
                                && OWNER.equals(((TopicEvent.TopicCreatedEvent) e).creatorId())
                                && TopicCategory.MOVIES == ((TopicEvent.TopicCreatedEvent) e).category()));
    }

    @Test
    void createTopic_withBlankName_rejects() {
        fixture.givenNoPriorActivity()
                .when(createCommand("   ", null))
                .expectException(TopicProblems.TopicNameEmptyProblem.class);
    }

    @Test
    void createTopic_withNameTooLong_rejects() {
        fixture.givenNoPriorActivity()
                .when(createCommand("a".repeat(256), null))
                .expectException(TopicProblems.TopicNameTooLongProblem.class);
    }

    @Test
    void createTopic_withDescriptionTooLong_rejects() {
        fixture.givenNoPriorActivity()
                .when(createCommand("Cinéma", "a".repeat(501)))
                .expectException(TopicProblems.TopicDescriptionTooLongProblem.class);
    }

    @Test
    void publishTopic_byOwner_appliesTopicPublishedEvent() {
        when(questionRepository.countApprovedByTopicId(TOPIC_ID)).thenReturn(7);

        fixture.given(createdTopic())
                .when(new TopicCommand.PublishTopicCommand(TOPIC_ID, OWNER))
                .expectEventsMatching(QuizUpAxonMatchers.singlePayloadMatching(
                        TopicEvent.TopicPublishedEvent.class,
                        e -> TOPIC_ID.equals(((TopicEvent.TopicPublishedEvent) e).topicId())));
    }

    @Test
    void publishTopic_byNonOwner_rejects() {
        fixture.given(createdTopic())
                .when(new TopicCommand.PublishTopicCommand(TOPIC_ID, "intruder"))
                .expectException(TopicProblems.TopicNotOwnerProblem.class);
    }

    @Test
    void publishTopic_withoutEnoughApprovedQuestions_rejects() {
        when(questionRepository.countApprovedByTopicId(TOPIC_ID)).thenReturn(3);

        fixture.given(createdTopic())
                .when(new TopicCommand.PublishTopicCommand(TOPIC_ID, OWNER))
                .expectException(QuestionProblems.NotEnoughApprovedQuestionsProblem.class);
    }

    @Test
    void updateName_byOwner_appliesEvent() {
        fixture.given(createdTopic())
                .when(new TopicCommand.UpdateTopicNameCommand(TOPIC_ID, OWNER, Language.FR, "Séries"))
                .expectEventsMatching(QuizUpAxonMatchers.singlePayloadMatching(
                        TopicEvent.TopicNameUpdatedEvent.class,
                        e -> "Séries".equals(((TopicEvent.TopicNameUpdatedEvent) e).name())));
    }

    @Test
    void updateName_byNonOwner_rejects() {
        fixture.given(createdTopic())
                .when(new TopicCommand.UpdateTopicNameCommand(TOPIC_ID, "intruder", Language.FR, "Séries"))
                .expectException(TopicProblems.TopicNotOwnerProblem.class);
    }

    @Test
    void updateName_withSameValue_emitsNoEvent() {
        fixture.given(createdTopic())
                .when(new TopicCommand.UpdateTopicNameCommand(TOPIC_ID, OWNER, Language.FR, "Cinéma"))
                .expectNoEvents();
    }

    @Test
    void updateName_withTooLongValue_rejects() {
        fixture.given(createdTopic())
                .when(new TopicCommand.UpdateTopicNameCommand(TOPIC_ID, OWNER, Language.FR, "a".repeat(256)))
                .expectException(TopicProblems.TopicNameTooLongProblem.class);
    }

    @Test
    void updateDescription_appliesEvent() {
        fixture.given(createdTopic())
                .when(new TopicCommand.UpdateTopicDescriptionCommand(TOPIC_ID, OWNER, "Nouvelle description"))
                .expectEventsMatching(QuizUpAxonMatchers.singlePayloadMatching(
                        TopicEvent.TopicDescriptionUpdatedEvent.class,
                        e -> "Nouvelle description"
                                .equals(((TopicEvent.TopicDescriptionUpdatedEvent) e).description())));
    }

    @Test
    void updateCategory_withNull_rejects() {
        fixture.given(createdTopic())
                .when(new TopicCommand.UpdateTopicCategoryCommand(TOPIC_ID, OWNER, null))
                .expectException(TopicProblems.TopicCategoryEmptyProblem.class);
    }

    @Test
    void updateImageUrl_withTooLongValue_rejects() {
        fixture.given(createdTopic())
                .when(new TopicCommand.UpdateTopicImageUrlCommand(TOPIC_ID, OWNER, "https://x.test/" + "a".repeat(1100)))
                .expectException(TopicProblems.TopicImageUrlTooLongProblem.class);
    }

    private TopicCommand.CreateTopicCommand createCommand(String name, String description) {
        return new TopicCommand.CreateTopicCommand(
                TOPIC_ID, Map.of(Language.FR, name), description, TopicCategory.MOVIES, null, null, null, OWNER);
    }

    private TopicEvent.TopicCreatedEvent createdTopic() {
        return new TopicEvent.TopicCreatedEvent(
                TOPIC_ID, Map.of(Language.FR, "Cinéma"), "Tout le cinéma", TopicCategory.MOVIES,
                null, null, null, OWNER, NOW);
    }
}
