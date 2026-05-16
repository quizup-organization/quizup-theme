package io.github.quizup.topic.infrastructure.in.api.mapper;

import io.github.quizup.common.domain.model.search.PageResult;
import io.github.quizup.common.infrastructure.in.api.response.PageResponse;
import io.github.quizup.common.infrastructure.mapper.SearchResponseMapper;
import io.github.quizup.topic.domain.model.Question;
import io.github.quizup.topic.domain.model.Topic;
import io.github.quizup.topic.infrastructure.in.api.response.QuestionResponse;
import io.github.quizup.topic.infrastructure.in.api.response.TopicResponse;

import java.util.List;

public final class QuestionResponseMapper {

    private QuestionResponseMapper() {
    }

    public static QuestionResponse toResponse(Question question) {
        return new QuestionResponse(
                question.questionId(),
                question.topicId(),
                question.text(),
                question.answers(),
                question.correctAnswer(),
                question.status(),
                question.creatorId(),
                question.updatedBy(),
                question.createdAt(),
                question.updatedAt()
        );
    }

    public static List<QuestionResponse> toResponse(List<Question> questions) {
        return questions.stream().map(QuestionResponseMapper::toResponse).toList();
    }

    public static PageResponse<QuestionResponse> toResponse(PageResult<Question> pageResult) {
        return SearchResponseMapper.toSearchResponse(pageResult, QuestionResponseMapper::toResponse);
    }
}

