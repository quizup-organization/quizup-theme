package io.github.quizup.theme.application.projection;

import io.github.quizup.social.domain.event.TopicFollowerEvent;
import io.github.quizup.theme.domain.event.QuestionEvent;
import io.github.quizup.theme.domain.event.TopicEvent;
import io.github.quizup.theme.domain.model.QuestionStatus;
import io.github.quizup.theme.domain.model.Topic;
import io.github.quizup.theme.domain.model.TopicStatus;
import io.github.quizup.theme.domain.port.out.TopicRepositoryPort;
import org.axonframework.eventhandling.EventHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumMap;
import java.util.Map;

@Component
public class TopicProjection {

    private final TopicRepositoryPort topicRepositoryPort;

    public TopicProjection(TopicRepositoryPort topicRepositoryPort) {
        this.topicRepositoryPort = topicRepositoryPort;
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
        topicRepositoryPort.findById(event.topicId())
                .ifPresent(topic -> {
                    Map<QuestionStatus, Integer> updatedCounters = copyCounters(topic.questionsCounter());
                    updatedCounters.merge(QuestionStatus.PENDING, 1, Integer::sum);
                    topicRepositoryPort.save(
                            topic.toBuilder()
                                    .questionsCounter(updatedCounters)
                                    .updatedAt(event.createdAt())
                                    .build()
                    );
                });
    }

    @EventHandler
    @Transactional
    public void on(QuestionEvent.QuestionApprovedEvent event) {
        topicRepositoryPort.findById(event.topicId())
                .ifPresent(topic -> {
                    Map<QuestionStatus, Integer> updatedCounters = copyCounters(topic.questionsCounter());
                    updatedCounters.compute(event.previousStatus(), (_, count) -> Math.max(0, safeCount(count) - 1));
                    updatedCounters.merge(QuestionStatus.APPROVED, 1, Integer::sum);
                    topicRepositoryPort.save(
                            topic.toBuilder()
                                    .questionsCounter(updatedCounters)
                                    .updatedAt(event.approvedAt())
                                    .build()
                    );
                });
    }

    @EventHandler
    @Transactional
    public void on(QuestionEvent.QuestionRejectedEvent event) {
        topicRepositoryPort.findById(event.topicId())
                .ifPresent(topic -> {
                    Map<QuestionStatus, Integer> updatedCounters = copyCounters(topic.questionsCounter());
                    updatedCounters.compute(event.previousStatus(), (_, count) -> Math.max(0, safeCount(count) - 1));
                    updatedCounters.merge(QuestionStatus.REJECTED, 1, Integer::sum);
                    topicRepositoryPort.save(
                            topic.toBuilder()
                                    .questionsCounter(updatedCounters)
                                    .updatedAt(event.rejectedAt())
                                    .build()
                    );
                });
    }

    @EventHandler
    @Transactional
    public void TopicFollowedEvent(TopicFollowerEvent.TopicFollowedEvent event) {
        topicRepositoryPort.findById(event.topicId())
                .ifPresent(topic -> topicRepositoryPort.save(
                        topic.toBuilder()
                                .followersCounter(topic.followersCounter() + 1)
                                .updatedAt(event.followedAt())
                                .build()
                ));
    }

    @EventHandler
    @Transactional
    public void TopicFollowedEvent(TopicFollowerEvent.TopicUnfollowedEvent event) {
        topicRepositoryPort.findById(event.topicId())
                .ifPresent(topic -> topicRepositoryPort.save(
                        topic.toBuilder()
                                .followersCounter(topic.followersCounter() - 1)
                                .updatedAt(event.unfollowedAt())
                                .build()
                ));
    }

    private static Map<QuestionStatus, Integer> copyCounters(Map<QuestionStatus, Integer> current) {
        Map<QuestionStatus, Integer> counters = new EnumMap<>(QuestionStatus.class);
        counters.put(QuestionStatus.PENDING, 0);
        counters.put(QuestionStatus.APPROVED, 0);
        counters.put(QuestionStatus.REJECTED, 0);
        if (current != null) {
            counters.putAll(current);
        }
        return counters;
    }

    private static int safeCount(Integer count) {
        return count == null ? 0 : count;
    }
}
