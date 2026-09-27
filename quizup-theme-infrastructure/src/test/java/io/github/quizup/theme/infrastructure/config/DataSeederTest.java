package io.github.quizup.theme.infrastructure.config;

import io.github.quizup.microservice.core.domain.constant.QuizUpConstants;
import io.github.quizup.theme.domain.model.Question;
import io.github.quizup.theme.domain.model.QuestionChoice;
import io.github.quizup.theme.domain.model.QuestionStatus;
import io.github.quizup.theme.domain.model.Topic;
import io.github.quizup.theme.domain.model.TopicCategory;
import io.github.quizup.theme.domain.model.TopicStatus;
import io.github.quizup.theme.domain.port.in.ApproveQuestionUseCase;
import io.github.quizup.theme.domain.port.in.CheckTopicUseCase;
import io.github.quizup.theme.domain.port.in.CreateQuestionUseCase;
import io.github.quizup.theme.domain.port.in.CreateTopicUseCase;
import io.github.quizup.theme.domain.port.in.GetQuestionUseCase;
import io.github.quizup.theme.domain.port.in.GetTopicUseCase;
import io.github.quizup.theme.domain.port.in.PublishTopicUseCase;
import io.github.quizup.theme.infrastructure.config.seed.QuestionSeedDefinition;
import io.github.quizup.theme.infrastructure.config.seed.SeedDataLoader;
import io.github.quizup.theme.infrastructure.config.seed.TopicSeedDefinition;
import org.axonframework.modelling.command.AggregateStreamCreationException;
import io.github.quizup.theme.infrastructure.properties.AppProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DataSeederTest {

    private static final String SYSTEM = QuizUpConstants.SYSTEM_USER_ID;

    @Mock
    private CheckTopicUseCase checkTopicUseCase;
    @Mock
    private CreateTopicUseCase createTopicUseCase;
    @Mock
    private CreateQuestionUseCase createQuestionUseCase;
    @Mock
    private ApproveQuestionUseCase approveQuestionUseCase;
    @Mock
    private PublishTopicUseCase publishTopicUseCase;
    @Mock
    private GetTopicUseCase getTopicUseCase;
    @Mock
    private GetQuestionUseCase getQuestionUseCase;
    @Mock
    private SeedDataLoader seedDataLoader;

    private DataSeeder seeder(boolean enabled) {
        return new DataSeeder(
                checkTopicUseCase,
                createTopicUseCase,
                createQuestionUseCase,
                approveQuestionUseCase,
                publishTopicUseCase,
                getTopicUseCase,
                getQuestionUseCase,
                seedDataLoader,
                new AppProperties(new AppProperties.SeedData(enabled, "ignored")));
    }

    @Test
    void doesNothingWhenDisabled() {
        seeder(false).run();

        verifyNoInteractions(checkTopicUseCase, createTopicUseCase, createQuestionUseCase,
                approveQuestionUseCase, publishTopicUseCase, getTopicUseCase,
                getQuestionUseCase, seedDataLoader);
    }

    @Test
    void createsMissingTopicQuestionsAndPublishes() {
        when(seedDataLoader.loadAll()).thenReturn(List.of(definition("topic-new", "Q1 ?", "Q2 ?")));
        when(checkTopicUseCase.existsByIdAndWait("topic-new")).thenReturn(false);
        when(getTopicUseCase.getById("topic-new"))
                .thenReturn(CompletableFuture.completedFuture(topic("topic-new", TopicStatus.DRAFT, 2)));
        when(getQuestionUseCase.getByTopicId("topic-new"))
                .thenReturn(CompletableFuture.completedFuture(List.of()));

        seeder(true).run();

        verify(createTopicUseCase).createAndWait(
                eq("topic-new"), eq("Nom"), eq("Description"), eq(TopicCategory.GENERAL),
                isNull(), isNull(), isNull(), eq(SYSTEM));
        verify(createQuestionUseCase, times(2)).createAndWait(
                anyString(), eq("topic-new"), anyString(), anyMap(), eq(QuestionChoice.A), isNull(), eq(SYSTEM));
        verify(approveQuestionUseCase, times(2)).approveAndWait(anyString(), eq(SYSTEM));
        verify(publishTopicUseCase).publishAndWait("topic-new", SYSTEM);
    }

    @Test
    void skipsAlreadyPublishedTopic() {
        when(seedDataLoader.loadAll()).thenReturn(List.of(definition("topic-published", "Q1 ?")));
        when(checkTopicUseCase.existsByIdAndWait("topic-published")).thenReturn(true);
        when(getTopicUseCase.getById("topic-published"))
                .thenReturn(CompletableFuture.completedFuture(topic("topic-published", TopicStatus.PUBLISHED, 20)));

        seeder(true).run();

        verifyNoInteractions(createTopicUseCase, createQuestionUseCase, approveQuestionUseCase,
                publishTopicUseCase, getQuestionUseCase);
    }

    @Test
    void repairsDraftTopicByCreatingMissingAndApprovingPendingQuestions() {
        when(seedDataLoader.loadAll())
                .thenReturn(List.of(definition("topic-draft", "Q1 ?", "Q2 ?", "Q3 ?")));
        when(checkTopicUseCase.existsByIdAndWait("topic-draft")).thenReturn(true);
        when(getTopicUseCase.getById("topic-draft"))
                .thenReturn(CompletableFuture.completedFuture(topic("topic-draft", TopicStatus.DRAFT, 3)));
        when(getQuestionUseCase.getByTopicId("topic-draft"))
                .thenReturn(CompletableFuture.completedFuture(List.of(
                        question("q-1", "Q1 ?", QuestionStatus.APPROVED),
                        question("q-2", "Q2 ?", QuestionStatus.PENDING))));

        seeder(true).run();

        verify(createQuestionUseCase, times(1)).createAndWait(
                anyString(), eq("topic-draft"), eq("Q3 ?"), anyMap(), eq(QuestionChoice.A), isNull(), eq(SYSTEM));
        verify(approveQuestionUseCase, times(2)).approveAndWait(anyString(), eq(SYSTEM));
        verify(approveQuestionUseCase).approveAndWait("q-2", SYSTEM);
        verify(publishTopicUseCase).publishAndWait("topic-draft", SYSTEM);
    }

    @Test
    void toleratesAggregateStreamCreationExceptionOnTopicCreation() {
        when(seedDataLoader.loadAll()).thenReturn(List.of(definition("topic-lag", "Q1 ?")));
        when(checkTopicUseCase.existsByIdAndWait("topic-lag")).thenReturn(false);
        doThrow(new CompletionException(new AggregateStreamCreationException("topic-lag")))
                .when(createTopicUseCase).createAndWait(
                        eq("topic-lag"), anyString(), anyString(), any(), isNull(), isNull(), isNull(), eq(SYSTEM));
        when(getTopicUseCase.getById("topic-lag"))
                .thenReturn(CompletableFuture.completedFuture(topic("topic-lag", TopicStatus.DRAFT, 1)));
        when(getQuestionUseCase.getByTopicId("topic-lag"))
                .thenReturn(CompletableFuture.completedFuture(List.of()));

        seeder(true).run();

        verify(publishTopicUseCase).publishAndWait("topic-lag", SYSTEM);
    }

    @Test
    void isolatesFailureBetweenTopics() {
        when(seedDataLoader.loadAll()).thenReturn(List.of(
                definition("topic-1", "Q1 ?"),
                definition("topic-2", "Q2 ?")));
        when(checkTopicUseCase.existsByIdAndWait("topic-1")).thenReturn(false);
        when(checkTopicUseCase.existsByIdAndWait("topic-2")).thenReturn(false);
        doThrow(new RuntimeException("boom"))
                .when(createTopicUseCase).createAndWait(
                        eq("topic-1"), anyString(), anyString(), any(), isNull(), isNull(), isNull(), eq(SYSTEM));
        when(getTopicUseCase.getById("topic-2"))
                .thenReturn(CompletableFuture.completedFuture(topic("topic-2", TopicStatus.DRAFT, 1)));
        when(getQuestionUseCase.getByTopicId("topic-2"))
                .thenReturn(CompletableFuture.completedFuture(List.of()));

        seeder(true).run();

        verify(publishTopicUseCase).publishAndWait("topic-2", SYSTEM);
        verify(publishTopicUseCase, never()).publishAndWait(eq("topic-1"), anyString());
    }

    private static TopicSeedDefinition definition(String topicId, String... texts) {
        List<QuestionSeedDefinition> questions = List.of(texts).stream()
                .map(DataSeederTest::seedQuestion)
                .toList();
        return new TopicSeedDefinition(topicId, "Nom", "Description", TopicCategory.GENERAL, null, questions);
    }

    private static QuestionSeedDefinition seedQuestion(String text) {
        Map<QuestionChoice, String> answers = new EnumMap<>(QuestionChoice.class);
        answers.put(QuestionChoice.A, "a");
        answers.put(QuestionChoice.B, "b");
        answers.put(QuestionChoice.C, "c");
        answers.put(QuestionChoice.D, "d");
        return new QuestionSeedDefinition(text, answers, QuestionChoice.A, null);
    }

    private static Topic topic(String topicId, TopicStatus status, int approvedCount) {
        Map<QuestionStatus, Integer> counters = new EnumMap<>(QuestionStatus.class);
        counters.put(QuestionStatus.APPROVED, approvedCount);
        return Topic.builder()
                .topicId(topicId)
                .name("Nom")
                .description("Description")
                .category(TopicCategory.GENERAL)
                .status(status)
                .creatorId(SYSTEM)
                .followersCounter(0)
                .questionsCounter(counters)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    private static Question question(String questionId, String text, QuestionStatus status) {
        return Question.builder()
                .questionId(questionId)
                .topicId("topic-draft")
                .text(text)
                .answers(Map.of())
                .correctAnswer(QuestionChoice.A)
                .status(status)
                .creatorId(SYSTEM)
                .createdAt(Instant.now())
                .build();
    }
}
