package io.github.quizup.theme.application.projection;

import io.github.quizup.microservice.core.domain.model.i18n.Language;
import io.github.quizup.theme.domain.command.QuestionCommand;
import io.github.quizup.theme.domain.event.QuestionReportEvent;
import io.github.quizup.theme.domain.model.Question;
import io.github.quizup.theme.domain.model.QuestionChoice;
import io.github.quizup.theme.domain.model.QuestionContent;
import io.github.quizup.theme.domain.model.QuestionReportReason;
import io.github.quizup.theme.domain.model.QuestionReportStatus;
import io.github.quizup.theme.domain.model.QuestionStatus;
import io.github.quizup.theme.domain.port.out.QuestionReportRepositoryPort;
import io.github.quizup.theme.domain.port.out.QuestionRepositoryPort;
import io.github.quizup.theme.infrastructure.properties.QuestionReportProperties;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class QuestionReportProjectionTest {

    private static final Instant NOW = Instant.parse("2026-10-17T12:00:00Z");

    private final QuestionReportRepositoryPort reportRepository = mock(QuestionReportRepositoryPort.class);
    private final QuestionRepositoryPort questionRepository = mock(QuestionRepositoryPort.class);
    private final CommandGateway commandGateway = mock(CommandGateway.class);
    private final QuestionReportProjection projection = new QuestionReportProjection(
            reportRepository, questionRepository, commandGateway,
            new QuestionReportProperties(new QuestionReportProperties.AutoSuspension(
                    true, 3, Duration.ofDays(7))));

    @BeforeEach
    void setUp() {
        when(questionRepository.findById("question-1")).thenReturn(Optional.of(question()));
    }

    @Test
    void thirdDistinctReporter_suspendsTheQuestion() {
        when(reportRepository.countDistinctOpenReportersSince(eq("question-1"), any()))
                .thenReturn(3);

        projection.on(reported());

        ArgumentCaptor<QuestionCommand.SuspendQuestionCommand> captor =
                ArgumentCaptor.forClass(QuestionCommand.SuspendQuestionCommand.class);
        verify(commandGateway).send(captor.capture());
        assertEquals("question-1", captor.getValue().questionId());
        assertEquals(3, captor.getValue().distinctReporters());
    }

    @Test
    void belowThreshold_doesNotSuspend() {
        when(reportRepository.countDistinctOpenReportersSince(eq("question-1"), any()))
                .thenReturn(2);

        projection.on(reported());

        verify(commandGateway, never()).send(any());
    }

    @Test
    void archivedQuestion_isNotSuspended() {
        Question archived = question().toBuilder().archived(true).build();
        when(questionRepository.findById("question-1")).thenReturn(Optional.of(archived));

        projection.on(reported());

        verify(commandGateway, never()).send(any());
    }

    private static QuestionReportEvent.QuestionReportedEvent reported() {
        return new QuestionReportEvent.QuestionReportedEvent(
                "thread-1", "report-3", "question-1", "topic-1", "creator-1",
                "reporter-3", "game-1", Set.of(QuestionReportReason.ANSWERS),
                "Bonne réponse fausse", Language.FR, null, NOW);
    }

    private static Question question() {
        return Question.builder()
                .questionId("question-1")
                .topicId("topic-1")
                .contents(Map.of(Language.FR, new QuestionContent(
                        Language.FR, "Capitale de la France ?",
                        Map.of(QuestionChoice.A, "Paris", QuestionChoice.B, "Lyon",
                                QuestionChoice.C, "Marseille", QuestionChoice.D, "Lille"))))
                .correctAnswer(QuestionChoice.A)
                .status(QuestionStatus.APPROVED)
                .creatorId("creator-1")
                .createdAt(NOW)
                .updatedAt(NOW)
                .suspended(false)
                .archived(false)
                .build();
    }
}
