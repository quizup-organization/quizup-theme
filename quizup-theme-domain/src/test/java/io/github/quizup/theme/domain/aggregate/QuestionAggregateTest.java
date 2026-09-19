package io.github.quizup.theme.domain.aggregate;

import io.github.quizup.axon.test.QuizUpAxonMatchers;
import io.github.quizup.theme.domain.command.QuestionCommand;
import io.github.quizup.theme.domain.event.QuestionEvent;
import io.github.quizup.theme.domain.exception.QuestionProblems;
import io.github.quizup.theme.domain.model.QuestionChoice;
import io.github.quizup.theme.domain.model.QuestionDifficulty;
import org.axonframework.test.aggregate.AggregateTestFixture;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

/**
 * Test Axon in-memory de l'agrégat {@link QuestionAggregate} via {@link AggregateTestFixture}.
 * <p>
 * 100 % in-memory : event store de l'agrégat en mémoire, aucun Postgres ni Axon Server.
 */
class QuestionAggregateTest {

    private final AggregateTestFixture<QuestionAggregate> fixture =
            new AggregateTestFixture<>(QuestionAggregate.class);

    @Test
    void createQuestion_appliesQuestionCreatedEvent() {
        // validateQuestionData() exige 4 réponses (A/B/C/D) : l'agrégat applique
        // QuestionCreatedEvent uniquement quand la validation passe.
        Map<QuestionChoice, String> answers = Map.of(
                QuestionChoice.A, "Paris",
                QuestionChoice.B, "Lyon",
                QuestionChoice.C, "Marseille",
                QuestionChoice.D, "Toulouse");

        fixture.givenNoPriorActivity()
                .when(new QuestionCommand.CreateQuestionCommand(
                        "q-1", "topic-1", "Capital of France?", answers, QuestionChoice.A,
                        "https://example.com/france.png", "creator-1"))
                .expectEventsMatching(QuizUpAxonMatchers.singlePayloadMatching(
                        QuestionEvent.QuestionCreatedEvent.class,
                        e -> ((QuestionEvent.QuestionCreatedEvent) e).questionId().equals("q-1")
                                && ((QuestionEvent.QuestionCreatedEvent) e).topicId().equals("topic-1")
                                && "https://example.com/france.png"
                                        .equals(((QuestionEvent.QuestionCreatedEvent) e).imageUrl())));
    }

    @Test
    void createQuestion_withTwoAnswers_rejects() {
        Map<QuestionChoice, String> badAnswers = Map.of(
                QuestionChoice.A, "Paris",
                QuestionChoice.B, "Lyon");

        fixture.givenNoPriorActivity()
                .when(new QuestionCommand.CreateQuestionCommand(
                        "q-bad", "topic-1", "Capital of France?", badAnswers, QuestionChoice.A,
                        null, "creator-1"))
                .expectException(QuestionProblems.QuestionAnswersInvalidProblem.class);
    }

    @Test
    void updateDifficulty_appliesQuestionDifficultyUpdatedEvent() {
        fixture.given(createdQuestion())
                .when(new QuestionCommand.UpdateQuestionDifficultyCommand(
                        "q-1", QuestionDifficulty.HARD))
                .expectEventsMatching(QuizUpAxonMatchers.singlePayloadMatching(
                        QuestionEvent.QuestionDifficultyUpdatedEvent.class,
                        e -> ((QuestionEvent.QuestionDifficultyUpdatedEvent) e).difficulty()
                                == QuestionDifficulty.HARD));
    }

    @Test
    void updateDifficulty_withSameValue_emitsNoEvent() {
        fixture.given(createdQuestion(),
                        new QuestionEvent.QuestionDifficultyUpdatedEvent(
                                "q-1", QuestionDifficulty.HARD, Instant.now()))
                .when(new QuestionCommand.UpdateQuestionDifficultyCommand(
                        "q-1", QuestionDifficulty.HARD))
                .expectNoEvents();
    }

    private QuestionEvent.QuestionCreatedEvent createdQuestion() {
        return new QuestionEvent.QuestionCreatedEvent(
                "q-1", "topic-1", "Capital of France?", answers(), QuestionChoice.A,
                null, "creator-1", Instant.now());
    }

    private Map<QuestionChoice, String> answers() {
        return Map.of(
                QuestionChoice.A, "Paris",
                QuestionChoice.B, "Lyon",
                QuestionChoice.C, "Marseille",
                QuestionChoice.D, "Toulouse");
    }
}
