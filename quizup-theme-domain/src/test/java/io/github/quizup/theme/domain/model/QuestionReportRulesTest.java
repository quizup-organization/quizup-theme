package io.github.quizup.theme.domain.model;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuestionReportRulesTest {

    private static final Instant NOW = Instant.parse("2026-10-17T12:00:00Z");

    @Test
    void windowStart_usesTheLatestModerationReset() {
        Instant reinstated = NOW.minus(Duration.ofDays(3));
        Instant restored = NOW.minus(Duration.ofDays(1));

        assertEquals(
                restored,
                QuestionReportRules.windowStart(
                        NOW, Duration.ofDays(7), reinstated, restored));
    }

    @Test
    void windowStart_fallsBackToTheConfiguredWindow() {
        assertEquals(
                NOW.minus(Duration.ofDays(7)),
                QuestionReportRules.windowStart(
                        NOW, Duration.ofDays(7), null, null));
    }

    @Test
    void shouldSuspend_respectsTheThreshold() {
        assertFalse(QuestionReportRules.shouldSuspend(2, 3));
        assertTrue(QuestionReportRules.shouldSuspend(3, 3));
        assertFalse(QuestionReportRules.shouldSuspend(3, 0));
    }
}
