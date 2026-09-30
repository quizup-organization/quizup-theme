package io.github.quizup.theme.domain.aggregate;

import io.github.quizup.axon.test.QuizUpAxonMatchers;
import io.github.quizup.microservice.core.domain.model.i18n.Language;
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

    private static final Instant NOW = Instant.parse("2026-09-20T10:00:00Z");

    private final AggregateTestFixture<QuestionAggregate> fixture =
            new AggregateTestFixture<>(QuestionAggregate.class);

    @Test
    void createQuestion_appliesQuestionCreatedEvent() {
        // validateQuestionData() exige 4 réponses (A/B/C/D) : l'agrégat applique
        // QuestionCreatedEvent uniquement quand la validation passe.
        fixture.givenNoPriorActivity()
                .when(new QuestionCommand.CreateQuestionCommand(
                        "q-1", "topic-1", "Capital of France?", answers(), QuestionChoice.A,
                        "https://example.com/france.png", "creator-1"))
                .expectEventsMatching(QuizUpAxonMatchers.singlePayloadMatching(
                        QuestionEvent.QuestionCreatedEvent.class,
                        e -> "q-1".equals(((QuestionEvent.QuestionCreatedEvent) e).questionId())
                                && "topic-1".equals(((QuestionEvent.QuestionCreatedEvent) e).topicId())
                                && Language.FR == ((QuestionEvent.QuestionCreatedEvent) e).sourceLanguage()
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
    void addTranslation_appliesQuestionTranslationAddedEvent() {
        fixture.given(createdQuestion())
                .when(new QuestionCommand.AddQuestionTranslationCommand(
                        "q-1", Language.EN, "What is the capital of France?", englishAnswers(), "editor-1"))
                .expectEventsMatching(QuizUpAxonMatchers.singlePayloadMatching(
                        QuestionEvent.QuestionTranslationAddedEvent.class,
                        e -> Language.EN == ((QuestionEvent.QuestionTranslationAddedEvent) e).language()
                                && "What is the capital of France?"
                                        .equals(((QuestionEvent.QuestionTranslationAddedEvent) e).text())
                                && "editor-1".equals(((QuestionEvent.QuestionTranslationAddedEvent) e).updatedBy())));
    }

    @Test
    void addTranslationWithSourceLanguage_rejects() {
        fixture.given(createdQuestion())
                .when(new QuestionCommand.AddQuestionTranslationCommand(
                        "q-1", Language.FR, "Capital of France?", answers(), "editor-1"))
                .expectException(QuestionProblems.QuestionTranslationIsSourceProblem.class);
    }

    @Test
    void addTranslationWithSameContent_emitsNoEvent() {
        fixture.given(createdQuestion(),
                        new QuestionEvent.QuestionTranslationAddedEvent(
                                "q-1", Language.EN, "What is the capital of France?", englishAnswers(), "editor-1", NOW))
                .when(new QuestionCommand.AddQuestionTranslationCommand(
                        "q-1", Language.EN, "What is the capital of France?", englishAnswers(), "editor-1"))
                .expectNoEvents();
    }

    @Test
    void addTranslationWithMissingLanguage_rejects() {
        fixture.given(createdQuestion())
                .when(new QuestionCommand.AddQuestionTranslationCommand(
                        "q-1", null, "What is the capital of France?", englishAnswers(), "editor-1"))
                .expectException(QuestionProblems.QuestionTranslationLanguageMissingProblem.class);
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
                "q-1", "topic-1", Language.FR, "Capital of France?", answers(), QuestionChoice.A,
                null, "creator-1", NOW);
    }

    private Map<QuestionChoice, String> answers() {
        return Map.of(
                QuestionChoice.A, "Paris",
                QuestionChoice.B, "Lyon",
                QuestionChoice.C, "Marseille",
                QuestionChoice.D, "Toulouse");
    }

    private Map<QuestionChoice, String> englishAnswers() {
        return Map.of(
                QuestionChoice.A, "Paris",
                QuestionChoice.B, "Lyon",
                QuestionChoice.C, "Marseille",
                QuestionChoice.D, "Toulouse");
    }
}
