package io.github.quizup.theme.domain.port.out;

/**
 * Port sortant - journal des réponses déjà comptées pour la difficulté.
 *
 * <p>La clé métier {@code (gameId, round, playerId)} garantit qu'un même événement
 * {@code QuestionAnsweredEvent} rejoué n'est compté qu'une fois.</p>
 */
public interface QuestionAnswerRecordPort {

    /**
     * Enregistre une réponse humaine.
     *
     * @return {@code true} si la réponse est nouvelle (à compter), {@code false} si déjà enregistrée.
     */
    boolean record(String gameId, String round, String playerId, String questionId, boolean correct);
}
