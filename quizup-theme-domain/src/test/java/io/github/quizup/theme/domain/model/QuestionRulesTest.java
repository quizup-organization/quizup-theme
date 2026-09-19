package io.github.quizup.theme.domain.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class QuestionRulesTest {

    @Test
    void belowMinAnswers_hasNoDifficulty() {
        assertNull(QuestionRules.difficultyFor(0, 0));
        assertNull(QuestionRules.difficultyFor(9, 9));
        assertNull(QuestionRules.difficultyFor(10, QuestionRules.MIN_ANSWERS_FOR_DIFFICULTY - 1));
    }

    @Test
    void derivesDifficultyFromCorrectRatio() {
        assertEquals(QuestionDifficulty.EASY, QuestionRules.difficultyFor(8, 10));
        assertEquals(QuestionDifficulty.MEDIUM, QuestionRules.difficultyFor(7, 10));
        assertEquals(QuestionDifficulty.HARD, QuestionRules.difficultyFor(5, 10));
        assertEquals(QuestionDifficulty.EXPERT, QuestionRules.difficultyFor(3, 10));
    }

    @Test
    void appliesThresholdBoundaries() {
        assertEquals(QuestionDifficulty.EASY, QuestionRules.difficultyFor(80, 100));
        assertEquals(QuestionDifficulty.MEDIUM, QuestionRules.difficultyFor(79, 100));
        assertEquals(QuestionDifficulty.MEDIUM, QuestionRules.difficultyFor(60, 100));
        assertEquals(QuestionDifficulty.HARD, QuestionRules.difficultyFor(59, 100));
        assertEquals(QuestionDifficulty.HARD, QuestionRules.difficultyFor(40, 100));
        assertEquals(QuestionDifficulty.EXPERT, QuestionRules.difficultyFor(39, 100));
    }
}
