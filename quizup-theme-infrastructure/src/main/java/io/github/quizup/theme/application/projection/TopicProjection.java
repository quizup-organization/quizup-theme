package io.github.quizup.theme.application.projection;

import io.github.quizup.theme.domain.event.QuestionEvent;
import io.github.quizup.theme.domain.event.TopicEvent;
import io.github.quizup.theme.domain.model.Topic;
import io.github.quizup.theme.domain.model.TopicStatus;
import io.github.quizup.theme.domain.port.out.TopicRepositoryPort;
import org.axonframework.eventhandling.EventHandler;
import org.springframework.stereotype.Component;

@Component
public class TopicProjection {

    private final TopicRepositoryPort topicRepositoryPort;

    public TopicProjection(TopicRepositoryPort topicRepositoryPort) {
        this.topicRepositoryPort = topicRepositoryPort;
    }

    @EventHandler
    public void on(TopicEvent.TopicCreatedEvent event) {
        Topic topic = new Topic(
                event.topicId(),
                event.name(),
                event.description(),
                event.category(),
                TopicStatus.DRAFT,
                event.creatorId(),
                event.creatorId(),
                0,
                event.createdAt(),
                event.createdAt()
        );
        topicRepositoryPort.save(topic);
    }

    @EventHandler
    public void on(QuestionEvent.QuestionCreatedEvent event) {
        topicRepositoryPort.findById(event.topicId())
                .ifPresent(topic -> topicRepositoryPort.save(
                        topic.toBuilder()
                                .questionCount(topic.questionCount() + 1)
                                .build()
                ));
    }

    @EventHandler
    public void on(TopicEvent.TopicPublishedEvent event) {
        topicRepositoryPort.findById(event.topicId())
                .ifPresent(topic -> topicRepositoryPort.save(
                        topic
                                .toBuilder()
                                .status(TopicStatus.PUBLISHED)
                                .updatedBy(event.updatedBy())
                                .updatedAt(event.publishedAt())
                                .build()
                ));
    }
}
