package io.github.quizup.theme.domain.port.out;

/**
 * Port sortant - ensemble des abonnés d'un sujet (clé naturelle {@code topicId + userId}).
 *
 * <p>Sert de source de vérité au recalcul de {@code Topic.followersCounter} : l'opération est
 * idempotente (upsert/delete par clé) et donc rejouable sans dérive.</p>
 */
public interface TopicFollowerRefRepositoryPort {

    void add(String topicId, String userId);

    void remove(String topicId, String userId);

    int countByTopicId(String topicId);
}
