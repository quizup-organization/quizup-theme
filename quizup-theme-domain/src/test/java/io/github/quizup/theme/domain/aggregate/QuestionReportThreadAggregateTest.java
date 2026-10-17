package io.github.quizup.theme.domain.aggregate;

import io.github.quizup.axon.test.QuizUpAxonMatchers;
import io.github.quizup.microservice.core.domain.model.i18n.Language;
import io.github.quizup.theme.domain.command.QuestionReportCommand;
import io.github.quizup.theme.domain.event.QuestionReportEvent;
import io.github.quizup.theme.domain.model.QuestionReportReason;
import io.github.quizup.theme.domain.model.QuestionReportResolution;
import io.github.quizup.theme.domain.model.QuestionReportResult;
import io.github.quizup.theme.domain.model.QuestionReportRules;
import io.github.quizup.theme.domain.model.QuestionReportSnapshot;
import io.github.quizup.theme.domain.model.QuestionReportSnapshotSource;
import io.github.quizup.theme.domain.model.QuestionReportStatus;
import org.axonframework.test.aggregate.AggregateTestFixture;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class QuestionReportThreadAggregateTest {

    private static final Instant NOW = Instant.parse("2026-10-17T10:00:00Z");
    private static final String THREAD =
            QuestionReportRules.threadId("question-1", "reporter-1");

    private final AggregateTestFixture<QuestionReportThreadAggregate> fixture =
            new AggregateTestFixture<>(QuestionReportThreadAggregate.class);

    @Test
    void report_createsAnOpenReport() {
        fixture.givenNoPriorActivity()
                .when(reportCommand())
                .expectEventsMatching(QuizUpAxonMatchers.singlePayloadMatching(
                        QuestionReportEvent.QuestionReportedEvent.class,
                        event -> {
                            QuestionReportEvent.QuestionReportedEvent reported =
                                    (QuestionReportEvent.QuestionReportedEvent) event;
                            return THREAD.equals(reported.threadId())
                                    && "question-1".equals(reported.questionId())
                                    && "reporter-1".equals(reported.reporterId());
                        }))
                .expectResultMessagePayloadMatching(
                        org.hamcrest.Matchers.instanceOf(QuestionReportResult.class));
    }

    @Test
    void report_isIdempotentWhileAnOpenReportExists() {
        fixture.given(reported("report-1"))
                .when(reportCommand())
                .expectNoEvents()
                .expectResultMessagePayload(new QuestionReportResult("report-1", false));
    }

    @Test
    void resolve_closesTheReport() {
        fixture.given(reported("report-1"))
                .when(new QuestionReportCommand.ResolveQuestionReportCommand(
                        THREAD, "report-1", QuestionReportResolution.RESOLVED,
                        "admin-1", "Corrigé", NOW.plusSeconds(60)))
                .expectEvents(new QuestionReportEvent.QuestionReportReviewedEvent(
                        THREAD, "report-1", QuestionReportStatus.RESOLVED,
                        "admin-1", "Corrigé", NOW.plusSeconds(60)));
    }

    @Test
    void report_afterResolution_opensANewReport() {
        fixture.given(
                        reported("report-1"),
                        new QuestionReportEvent.QuestionReportReviewedEvent(
                                THREAD, "report-1", QuestionReportStatus.RESOLVED,
                                "admin-1", "Corrigé", NOW.plusSeconds(60)))
                .when(reportCommand())
                .expectEventsMatching(QuizUpAxonMatchers.singlePayloadMatching(
                        QuestionReportEvent.QuestionReportedEvent.class,
                        event -> !"report-1".equals(
                                ((QuestionReportEvent.QuestionReportedEvent) event).reportId())))
                .expectResultMessagePayloadMatching(
                        org.hamcrest.Matchers.instanceOf(QuestionReportResult.class));
    }

    @Test
    void threadId_isStableAndPlayerScoped() {
        assertEquals(
                QuestionReportRules.threadId("q", "u1"),
                QuestionReportRules.threadId("q", "u1"));
        assertNotEquals(
                QuestionReportRules.threadId("q", "u1"),
                QuestionReportRules.threadId("q", "u2"));
    }

    private static QuestionReportEvent.QuestionReportedEvent reported(String reportId) {
        return new QuestionReportEvent.QuestionReportedEvent(
                THREAD, reportId, "question-1", "topic-1", "creator-1",
                "reporter-1", "game-1", Set.of(QuestionReportReason.ANSWERS),
                null, Language.FR, snapshot(), NOW);
    }

    private static QuestionReportCommand.ReportQuestionCommand reportCommand() {
        return new QuestionReportCommand.ReportQuestionCommand(
                THREAD, "question-1", "topic-1", "creator-1", "reporter-1", "game-1",
                Set.of(QuestionReportReason.ANSWERS), "La bonne réponse est fausse",
                Language.FR, snapshot(), NOW);
    }

    private static QuestionReportSnapshot snapshot() {
        return new QuestionReportSnapshot(
                "Capitale de la France ?", null, Map.of("A", "Paris", "B", "Lyon"),
                Map.of(), "A", "fr", QuestionReportSnapshotSource.PLAYED);
    }
}
