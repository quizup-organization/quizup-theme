package io.github.quizup.theme.infrastructure.out.persistence.entity;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Ensemble des abonnés d'un sujet (clé naturelle). Source du recalcul de
 * {@code topic_entry.followers_counter} (idempotent/rejouable).
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "topic_follower_ref")
public class TopicFollowerRefEntity {

    @EmbeddedId
    private TopicFollowerRefId id;

    public TopicFollowerRefEntity(TopicFollowerRefId id) {
        this.id = id;
    }
}
