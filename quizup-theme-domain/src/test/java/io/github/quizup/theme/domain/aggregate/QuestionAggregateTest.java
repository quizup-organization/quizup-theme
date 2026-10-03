package io.github.quizup.theme.domain.aggregate;

import io.github.quizup.axon.test.QuizUpAxonMatchers;
import io.github.quizup.microservice.core.domain.model.i18n.Language;
import io.github.quizup.theme.domain.command.QuestionCommand;
import io.github.quizup.theme.domain.event.QuestionEvent;
import io.github.quizup.theme.domain.exception.QuestionProblems;
import io.github.quizup.theme.domain.model.QuestionChoice;
import io.github.quizup.theme.domain.model.QuestionContent;
import io.github.quizup.theme.domain.model.QuestionDifficulty;
import org.axonframework.test.aggregate.AggregateTestFixture;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.EnumMap;
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
        fixture.givenNoPriorActivity()
                .when(new QuestionCommand.CreateQuestionCommand(
                        "q-1", "topic-1", frenchContents(), QuestionChoice.A,
                        "https://example.com/france.png", "creator-1"))
                .expectEventsMatching(QuizUpAxonMatchers.singlePayloadMatching(
                        QuestionEvent.QuestionCreatedEvent.class,
                        e -> "q-1".equals(((QuestionEvent.QuestionCreatedEvent) e).questionId())
                                && "topic-1".equals(((QuestionEvent.QuestionCreatedEvent) e).topicId())
                                && Language.FR == ((QuestionEvent.QuestionCreatedEvent) e).contents()
                                        .get(Language.FR).language()
                                && "https://example.com/france.png"
                                        .equals(((QuestionEvent.QuestionCreatedEvent) e).imageUrl())));
    }

    @Test
    void createQuestion_withFrenchAndEnglish_appliesBothContents() {
        fixture.givenNoPriorActivity()
                .when(new QuestionCommand.CreateQuestionCommand(
                        "q-1", "topic-1", frenchAndEnglishContents(), QuestionChoice.A,
                        null, "creator-1"))
                .expectEventsMatching(QuizUpAxonMatchers.singlePayloadMatching(
                        QuestionEvent.QuestionCreatedEvent.class,
                        e -> ((QuestionEvent.QuestionCreatedEvent) e).contents().keySet()
                                .containsAll(java.util.Set.of(Language.FR, Language.EN))));
    }

    @Test
    void createQuestion_withoutContents_rejects() {
        fixture.givenNoPriorActivity()
                .when(new QuestionCommand.CreateQuestionCommand(
                        "q-1", "topic-1", Map.of(), QuestionChoice.A, null, "creator-1"))
                .expectException(QuestionProblems.QuestionContentsEmptyProblem.class);
    }

    @Test
    void createQuestion_withLanguageMismatch_rejects() {
        fixture.givenNoPriorActivity()
                .when(new QuestionCommand.CreateQuestionCommand(
                        "q-1", "topic-1",
                        Map.of(Language.FR, new QuestionContent(Language.EN, "Capital of France?", answers())),
                        QuestionChoice.A, null, "creator-1"))
                .expectException(QuestionProblems.QuestionContentLanguageMismatchProblem.class);
    }

    @Test
    void createQuestion_withTwoAnswers_rejects() {
        Map<QuestionChoice, String> badAnswers = Map.of(
                QuestionChoice.A, "Paris",
                QuestionChoice.B, "Lyon");

        fixture.givenNoPriorActivity()
                .when(new QuestionCommand.CreateQuestionCommand(
                        "q-bad", "topic-1",
                        Map.of(Language.FR, new QuestionContent(Language.FR, "Capital of France?", badAnswers)),
                        QuestionChoice.A, null, "creator-1"))
                .expectException(QuestionProblems.QuestionAnswersInvalidProblem.class);
    }

    @Test
    void addTranslations_appliesQuestionTranslationsAddedEvent() {
        fixture.given(createdQuestion())
                .when(new QuestionCommand.AddQuestionTranslationsCommand(
                        "q-1", englishOnlyContents(), "editor-1"))
                .expectEventsMatching(QuizUpAxonMatchers.singlePayloadMatching(
                        QuestionEvent.QuestionTranslationsAddedEvent.class,
                        e -> ((QuestionEvent.QuestionTranslationsAddedEvent) e).contents().containsKey(Language.EN)
                                && "editor-1".equals(((QuestionEvent.QuestionTranslationsAddedEvent) e).updatedBy())));
    }

    @Test
    void addTranslations_withSameContent_emitsNoEvent() {
        fixture.given(createdQuestion())
                .when(new QuestionCommand.AddQuestionTranslationsCommand(
                        "q-1", frenchContents(), "editor-1"))
                .expectNoEvents();
    }

    @Test
    void addTranslations_withoutContents_rejects() {
        fixture.given(createdQuestion())
                .when(new QuestionCommand.AddQuestionTranslationsCommand("q-1", Map.of(), "editor-1"))
                .expectException(QuestionProblems.QuestionContentsEmptyProblem.class);
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

    @Test
    void addTranslation_appliesQuestionTranslationAddedEvent() {
        fixture.given(createdQuestion())
                .when(new QuestionCommand.AddQuestionTranslationCommand(
                        "q-1", "editor-1", Language.EN,
                        "What is the capital of France?", answers()))
                .expectEventsMatching(QuizUpAxonMatchers.singlePayloadMatching(
                        QuestionEvent.QuestionTranslationAddedEvent.class,
                        e -> Language.EN == ((QuestionEvent.QuestionTranslationAddedEvent) e).language()
                                && "editor-1".equals(((QuestionEvent.QuestionTranslationAddedEvent) e).updatedBy())));
    }

    @Test
    void addTranslation_withExistingLanguage_rejects() {
        fixture.given(createdQuestion())
                .when(new QuestionCommand.AddQuestionTranslationCommand(
                        "q-1", "editor-1", Language.FR, "Autre texte", answers()))
                .expectException(QuestionProblems.QuestionTranslationAlreadyExistsProblem.class);
    }

    @Test
    void addTranslation_withTextTooLong_rejects() {
        fixture.given(createdQuestion())
                .when(new QuestionCommand.AddQuestionTranslationCommand(
                        "q-1", "editor-1", Language.EN, "a".repeat(256), answers()))
                .expectException(QuestionProblems.QuestionTextTooLongProblem.class);
    }

    @Test
    void updateText_appliesEvent() {
        fixture.given(createdQuestion())
                .when(new QuestionCommand.UpdateQuestionTextCommand(
                        "q-1", "editor-1", Language.FR, "Quelle est la capitale de la France ?"))
                .expectEventsMatching(QuizUpAxonMatchers.singlePayloadMatching(
                        QuestionEvent.QuestionTextUpdatedEvent.class,
                        e -> "Quelle est la capitale de la France ?"
                                .equals(((QuestionEvent.QuestionTextUpdatedEvent) e).text())));
    }

    @Test
    void updateText_withUnknownLanguage_rejects() {
        fixture.given(createdQuestion())
                .when(new QuestionCommand.UpdateQuestionTextCommand(
                        "q-1", "editor-1", Language.EN, "What is the capital of France?"))
                .expectException(QuestionProblems.QuestionLanguageNotFoundProblem.class);
    }

    @Test
    void updateText_withSameValue_emitsNoEvent() {
        fixture.given(createdQuestion())
                .when(new QuestionCommand.UpdateQuestionTextCommand(
                        "q-1", "editor-1", Language.FR, "Capitale de la France ?"))
                .expectNoEvents();
    }

    @Test
    void updateAnswers_appliesEvent() {
        Map<QuestionChoice, String> answers = Map.of(
                QuestionChoice.A, "Paris",
                QuestionChoice.B, "Lyon",
                QuestionChoice.C, "Marseille",
                QuestionChoice.D, "Lille");

        fixture.given(createdQuestion())
                .when(new QuestionCommand.UpdateQuestionAnswersCommand(
                        "q-1", "editor-1", Language.FR, answers))
                .expectEventsMatching(QuizUpAxonMatchers.singlePayloadMatching(
                        QuestionEvent.QuestionAnswersUpdatedEvent.class,
                        e -> "Lille".equals(((QuestionEvent.QuestionAnswersUpdatedEvent) e).answers()
                                .get(QuestionChoice.D))));
    }

    @Test
    void updateAnswers_withThreeAnswers_rejects() {
        Map<QuestionChoice, String> answers = Map.of(
                QuestionChoice.A, "Paris",
                QuestionChoice.B, "Lyon",
                QuestionChoice.C, "Marseille");

        fixture.given(createdQuestion())
                .when(new QuestionCommand.UpdateQuestionAnswersCommand(
                        "q-1", "editor-1", Language.FR, answers))
                .expectException(QuestionProblems.QuestionAnswersInvalidProblem.class);
    }

    @Test
    void updateCorrectAnswer_appliesEvent() {
        fixture.given(createdQuestion())
                .when(new QuestionCommand.UpdateQuestionCorrectAnswerCommand(
                        "q-1", "editor-1", QuestionChoice.B))
                .expectEventsMatching(QuizUpAxonMatchers.singlePayloadMatching(
                        QuestionEvent.QuestionCorrectAnswerUpdatedEvent.class,
                        e -> QuestionChoice.B == ((QuestionEvent.QuestionCorrectAnswerUpdatedEvent) e)
                                .correctAnswer()));
    }

    @Test
    void updateCorrectAnswer_withSameValue_emitsNoEvent() {
        fixture.given(createdQuestion())
                .when(new QuestionCommand.UpdateQuestionCorrectAnswerCommand(
                        "q-1", "editor-1", QuestionChoice.A))
                .expectNoEvents();
    }

    @Test
    void updateImageUrl_appliesEvent() {
        fixture.given(createdQuestion())
                .when(new QuestionCommand.UpdateQuestionImageUrlCommand(
                        "q-1", "editor-1", "https://example.com/new.png"))
                .expectEventsMatching(QuizUpAxonMatchers.singlePayloadMatching(
                        QuestionEvent.QuestionImageUrlUpdatedEvent.class,
                        e -> "https://example.com/new.png"
                                .equals(((QuestionEvent.QuestionImageUrlUpdatedEvent) e).imageUrl())));
    }

    @Test
    void updateImageUrl_withTooLongValue_rejects() {
        fixture.given(createdQuestion())
                .when(new QuestionCommand.UpdateQuestionImageUrlCommand(
                        "q-1", "editor-1", "https://x.test/" + "a".repeat(1100)))
                .expectException(QuestionProblems.QuestionImageUrlTooLongProblem.class);
    }

    private QuestionEvent.QuestionCreatedEvent createdQuestion() {
        return new QuestionEvent.QuestionCreatedEvent(
                "q-1", "topic-1", frenchContents(), QuestionChoice.A,
                null, "creator-1", NOW);
    }

    private Map<Language, QuestionContent> frenchContents() {
        return Map.of(Language.FR, new QuestionContent(Language.FR, "Capitale de la France ?", answers()));
    }

    private Map<Language, QuestionContent> englishOnlyContents() {
        return Map.of(Language.EN,
                new QuestionContent(Language.EN, "What is the capital of France?", answers()));
    }

    private Map<Language, QuestionContent> frenchAndEnglishContents() {
        Map<Language, QuestionContent> contents = new EnumMap<>(Language.class);
        contents.put(Language.FR, new QuestionContent(Language.FR, "Capitale de la France ?", answers()));
        contents.put(Language.EN, new QuestionContent(Language.EN, "What is the capital of France?", answers()));
        return contents;
    }

    private Map<QuestionChoice, String> answers() {
        return Map.of(
                QuestionChoice.A, "Paris",
                QuestionChoice.B, "Lyon",
                QuestionChoice.C, "Marseille",
                QuestionChoice.D, "Toulouse");
    }
}
