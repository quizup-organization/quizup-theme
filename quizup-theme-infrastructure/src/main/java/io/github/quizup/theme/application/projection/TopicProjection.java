package io.github.quizup.theme.application.projection;

import io.github.quizup.social.domain.event.TopicFollowerEvent;
import io.github.quizup.theme.domain.event.QuestionEvent;
import io.github.quizup.theme.domain.event.TopicEvent;
import io.github.quizup.theme.domain.model.QuestionStatus;
import io.github.quizup.theme.domain.model.Topic;
import io.github.quizup.theme.domain.model.TopicStatus;
import io.github.quizup.theme.domain.port.out.QuestionRepositoryPort;
import io.github.quizup.theme.domain.port.out.TopicFollowerRefRepositoryPort;
import io.github.quizup.theme.domain.port.out.TopicRepositoryPort;
import org.axonframework.config.ProcessingGroup;
import org.axonframework.eventhandling.EventHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.EnumMap;
import java.util.Map;

/**
 * Projection des thèmes.
 *
 * <p>Les compteurs ({@code followersCounter}, {@code questionsCounter}) sont <b>recalculés</b>
 * à partir de leur source (ensemble d'abonnés / questions par statut) plutôt qu'incrémentés :
 * l'opération est idempotente et rejouable sans dérive.</p>
 */
@Component
@ProcessingGroup("topic-projection")
public class TopicProjection {

    private final TopicRepositoryPort topicRepositoryPort;
    private final TopicFollowerRefRepositoryPort followerRefRepositoryPort;
    private final QuestionRepositoryPort questionRepositoryPort;

    public TopicProjection(TopicRepositoryPort topicRepositoryPort,
                           TopicFollowerRefRepositoryPort followerRefRepositoryPort,
                           QuestionRepositoryPort questionRepositoryPort) {
        this.topicRepositoryPort = topicRepositoryPort;
        this.followerRefRepositoryPort = followerRefRepositoryPort;
        this.questionRepositoryPort = questionRepositoryPort;
    }

    @EventHandler
    @Transactional
    public void on(TopicEvent.TopicCreatedEvent event) {
        Map<QuestionStatus, Integer> questionsCounters = new EnumMap<>(QuestionStatus.class);
        questionsCounters.put(QuestionStatus.PENDING, 0);
        questionsCounters.put(QuestionStatus.APPROVED, 0);
        questionsCounters.put(QuestionStatus.REJECTED, 0);

        Topic topic = new Topic(
                event.topicId(),
                event.name(),
                event.description(),
                event.category(),
                TopicStatus.DRAFT,
                event.creatorId(),
                event.creatorId(),
                0,
                questionsCounters,
                event.emoji(),
                event.color(),
                event.createdAt(),
                event.createdAt()
        );
        topicRepositoryPort.save(topic);
    }

    @EventHandler
    @Transactional
    public void on(TopicEvent.TopicPublishedEvent event) {
        topicRepositoryPort.findById(event.topicId())
                .ifPresent(topic -> topicRepositoryPort.save(
                        topic.toBuilder()
                                .status(TopicStatus.PUBLISHED)
                                .updatedBy(event.updatedBy())
                                .updatedAt(event.publishedAt())
                                .build()
                ));
    }

    @EventHandler
    @Transactional
    public void on(QuestionEvent.QuestionCreatedEvent event) {
        refreshQuestionsCounter(event.topicId(), event.createdAt());
    }

    @EventHandler
    @Transactional
    public void on(QuestionEvent.QuestionApprovedEvent event) {
        refreshQuestionsCounter(event.topicId(), event.approvedAt());
    }

    @EventHandler
    @Transactional
    public void on(QuestionEvent.QuestionRejectedEvent event) {
        refreshQuestionsCounter(event.topicId(), event.rejectedAt());
    }

    @EventHandler
    @Transactional
    public void on(TopicFollowerEvent.TopicFollowedEvent event) {
        topicRepositoryPort.findById(event.topicId()).ifPresent(topic -> {
            followerRefRepositoryPort.add(event.topicId(), event.userId());
            topicRepositoryPort.save(topic.toBuilder()
                    .followersCounter(followerRefRepositoryPort.countByTopicId(event.topicId()))
                    .updatedAt(event.followedAt())
                    .build());
        });
    }

    @EventHandler
    @Transactional
    public void on(TopicFollowerEvent.TopicUnfollowedEvent event) {
        topicRepositoryPort.findById(event.topicId()).ifPresent(topic -> {
            followerRefRepositoryPort.remove(event.topicId(), event.userId());
            topicRepositoryPort.save(topic.toBuilder()
                    .followersCounter(followerRefRepositoryPort.countByTopicId(event.topicId()))
                    .updatedAt(event.unfollowedAt())
                    .build());
        });
    }

    private void refreshQuestionsCounter(String topicId, Instant updatedAt) {
        Map<QuestionStatus, Integer> counters = new EnumMap<>(QuestionStatus.class);
        for (QuestionStatus status : QuestionStatus.values()) {
            counters.put(status, questionRepositoryPort.countByTopicIdAndStatus(topicId, status));
        }
        topicRepositoryPort.findById(topicId)
                .ifPresent(topic -> topicRepositoryPort.save(
                        topic.toBuilder()
                                .questionsCounter(counters)
                                .updatedAt(updatedAt)
                                .build()
                ));
    }
}
