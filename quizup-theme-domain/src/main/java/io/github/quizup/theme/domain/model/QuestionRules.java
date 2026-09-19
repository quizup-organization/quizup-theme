package io.github.quizup.theme.domain.model;

/**
 * Règles de calcul de la difficulté d'une question.
 *
 * <p>La difficulté est <b>déduite</b> du taux de bonnes réponses observé. Elle reste
 * {@code null} tant que l'échantillon est insuffisant. Les non-réponses (timeouts) sont des
 * réponses fausses.
 */
public interface QuestionRules {

    /** Nombre minimal de réponses avant de publier une difficulté. */
    int MIN_ANSWERS_FOR_DIFFICULTY = 10;

    int EASY_THRESHOLD = 80;
    int MEDIUM_THRESHOLD = 60;
    int HARD_THRESHOLD = 40;

    /**
     * @return la difficulté déduite, ou {@code null} si {@code totalAnswers} est sous le seuil
     * minimal.
     */
    static QuestionDifficulty difficultyFor(int correctAnswers, int totalAnswers) {
        if (totalAnswers < MIN_ANSWERS_FOR_DIFFICULTY) {
            return null;
        }

        double percentage = 100.0 * correctAnswers / totalAnswers;

        if (percentage >= EASY_THRESHOLD) {
            return QuestionDifficulty.EASY;
        }
        if (percentage >= MEDIUM_THRESHOLD) {
            return QuestionDifficulty.MEDIUM;
        }
        if (percentage >= HARD_THRESHOLD) {
            return QuestionDifficulty.HARD;
        }
        return QuestionDifficulty.EXPERT;
    }
}
