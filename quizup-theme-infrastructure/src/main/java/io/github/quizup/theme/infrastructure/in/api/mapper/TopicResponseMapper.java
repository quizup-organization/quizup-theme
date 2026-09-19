package io.github.quizup.theme.infrastructure.in.api.mapper;

import io.github.quizup.microservice.core.domain.model.search.PageResult;
import io.github.quizup.microservice.core.infrastructure.in.api.response.PageResponse;
import io.github.quizup.microservice.core.infrastructure.mapper.SearchResponseMapper;
import io.github.quizup.theme.domain.model.Topic;
import io.github.quizup.theme.domain.model.TopicCategory;
import io.github.quizup.theme.infrastructure.in.api.response.TopicCategoryResponse;
import io.github.quizup.theme.infrastructure.in.api.response.TopicResponse;

import java.util.List;

public final class TopicResponseMapper {

    private TopicResponseMapper() {
    }

    public static TopicResponse toResponse(Topic topic) {
        return new TopicResponse(
                topic.topicId(),
                topic.name(),
                topic.description(),
                topic.category(),
                topic.status(),
                topic.creatorId(),
                topic.updatedBy(),
                topic.followersCounter(),
                topic.questionsCounter(),
                topic.emoji(),
                topic.color(),
                topic.createdAt(),
                topic.updatedAt()
        );
    }

    public static List<TopicResponse> toResponse(List<Topic> topics) {
        return topics.stream().map(TopicResponseMapper::toResponse).toList();
    }

    public static PageResponse<TopicResponse> toResponse(PageResult<Topic> pageResult) {
        return SearchResponseMapper.toSearchResponse(pageResult, TopicResponseMapper::toResponse);
    }

    public static TopicCategoryResponse toResponse(TopicCategory category) {
        return new TopicCategoryResponse(category, category.label());
    }

    public static List<TopicCategoryResponse> toCategoryResponse(List<TopicCategory> categories) {
        return categories.stream().map(TopicResponseMapper::toResponse).toList();
    }
}
