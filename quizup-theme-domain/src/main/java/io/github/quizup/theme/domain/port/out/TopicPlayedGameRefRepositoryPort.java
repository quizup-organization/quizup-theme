package io.github.quizup.theme.domain.port.out;

/**
 * Port sortant - ensemble des parties connues par thème (clé naturelle {@code gameId}).
 *
 * <p>Sert de source de vérité au recalcul de {@code Topic.gamesCounter} : l'opération est
 * idempotente (une partie n'est comptée qu'une fois, même rejouée depuis le flux d'événements)
 * et donc rejouable sans dérive.</p>
 */
public interface TopicPlayedGameRefRepositoryPort {

    /** Enregistre la partie sur son thème (no-op si elle est déjà connue : rejeu). */
    void record(String gameId, String topicId);

    int countByTopicId(String topicId);
}
