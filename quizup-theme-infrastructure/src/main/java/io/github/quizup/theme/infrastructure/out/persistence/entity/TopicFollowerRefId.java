package io.github.quizup.theme.infrastructure.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

/** Clé composite {@code (topic_id, user_id)} de {@link TopicFollowerRefEntity}. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@Embeddable
public class TopicFollowerRefId implements Serializable {

    @Column(name = "topic_id", length = 255, nullable = false)
    private String topicId;

    @Column(name = "user_id", length = 255, nullable = false)
    private String userId;
}
