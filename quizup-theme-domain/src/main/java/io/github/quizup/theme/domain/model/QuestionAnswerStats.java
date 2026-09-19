package io.github.quizup.theme.domain.model;

/**
 * Compteurs de réponses d'une question, base du calcul de difficulté.
 *
 * @param questionId  question observée
 * @param answerCount total des réponses enregistrées (réponses fausses et timeouts inclus)
 * @param correctCount sous-ensemble de {@code answerCount} ayant répondu correctement
 */
public record QuestionAnswerStats(
        String questionId,
        int answerCount,
        int correctCount
) {

    public static QuestionAnswerStats empty(String questionId) {
        return new QuestionAnswerStats(questionId, 0, 0);
    }

    public QuestionAnswerStats withAnswer(boolean correct) {
        return new QuestionAnswerStats(
                questionId,
                answerCount + 1,
                correct ? correctCount + 1 : correctCount
        );
    }
}
