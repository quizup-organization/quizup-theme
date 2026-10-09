package io.github.quizup.theme.infrastructure.out.persistence.mapper;

import io.github.quizup.microservice.core.domain.model.i18n.Language;
import io.github.quizup.theme.domain.model.Topic;
import io.github.quizup.theme.domain.util.SearchText;
import io.github.quizup.theme.infrastructure.out.persistence.entity.TopicEntity;

import java.util.EnumMap;
import java.util.Map;

public final class TopicEntityMapper {

    private TopicEntityMapper() {
    }

    public static Topic toDomain(TopicEntity entity) {
        Map<Language, String> names = new EnumMap<>(Language.class);
        if (entity.getNameFr() != null) {
            names.put(Language.FR, entity.getNameFr());
        }
        if (entity.getNameEn() != null) {
            names.put(Language.EN, entity.getNameEn());
        }
        return new Topic(
                entity.getTopicId(),
                names,
                entity.getDescription(),
                entity.getCategory(),
                entity.getStatus(),
                entity.getCreatorId(),
                entity.getUpdatedBy(),
                entity.getFollowersCounter(),
                entity.getQuestionsCounter(),
                entity.getEmoji(),
                entity.getColor(),
                entity.getImageUrl(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public static TopicEntity toEntity(Topic topic) {
        TopicEntity topicEntity = new TopicEntity();
        topicEntity.setTopicId(topic.topicId());
        Map<Language, String> names = topic.names() == null ? Map.of() : topic.names();
        String fr = names.get(Language.FR);
        String en = names.get(Language.EN);
        topicEntity.setNameFr(fr);
        topicEntity.setNameFrNormalized(SearchText.normalize(fr));
        topicEntity.setNameEn(en);
        topicEntity.setNameEnNormalized(SearchText.normalize(en));
        topicEntity.setDescription(topic.description());
        topicEntity.setCategory(topic.category());
        topicEntity.setStatus(topic.status());
        topicEntity.setCreatorId(topic.creatorId());
        topicEntity.setUpdatedBy(topic.updatedBy());
        topicEntity.setFollowersCounter(topic.followersCounter());
        topicEntity.setQuestionsCounter(topic.questionsCounter());
        topicEntity.setEmoji(topic.emoji());
        topicEntity.setColor(topic.color());
        topicEntity.setImageUrl(topic.imageUrl());
        topicEntity.setCreatedAt(topic.createdAt());
        topicEntity.setUpdatedAt(topic.updatedAt());
        return topicEntity;
    }
}
