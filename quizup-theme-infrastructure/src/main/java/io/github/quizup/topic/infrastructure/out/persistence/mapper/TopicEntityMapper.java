package io.github.quizup.topic.infrastructure.out.persistence.mapper;

import io.github.quizup.topic.domain.model.Topic;
import io.github.quizup.topic.infrastructure.out.persistence.entity.TopicEntity;

public final class TopicEntityMapper {

    private TopicEntityMapper() {
    }

    public static Topic toDomain(TopicEntity entity) {
        return new Topic(
                entity.getTopicId(),
                entity.getName(),
                entity.getDescription(),
                entity.getCategory(),
                entity.getStatus(),
                entity.getCreatorId(),
                entity.getUpdatedBy(),
                entity.getQuestionCount(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public static TopicEntity toEntity(Topic topic) {
        TopicEntity topicEntity = new TopicEntity();
        topicEntity.setTopicId(topic.topicId());
        topicEntity.setName(topic.name());
        topicEntity.setDescription(topic.description());
        topicEntity.setCategory(topic.category());
        topicEntity.setStatus(topic.status());
        topicEntity.setCreatorId(topic.creatorId());
        topicEntity.setUpdatedBy(topic.updatedBy());
        topicEntity.setQuestionCount(topic.questionCount());
        topicEntity.setCreatedAt(topic.createdAt());
        topicEntity.setUpdatedAt(topic.updatedAt());
        return topicEntity;
    }
}

