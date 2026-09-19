package io.github.quizup.theme.domain.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class QuestionAnswerStatsTest {

    @Test
    void withAnswer_incrementsTotalAndCorrectOnlyWhenCorrect() {
        QuestionAnswerStats stats = QuestionAnswerStats.empty("q-1")
                .withAnswer(true)
                .withAnswer(false)
                .withAnswer(true);

        assertEquals("q-1", stats.questionId());
        assertEquals(3, stats.answerCount());
        assertEquals(2, stats.correctCount());
    }
}
