package io.github.quizup.theme.infrastructure.in.api.mapper;

import io.github.quizup.microservice.core.domain.model.search.PageResult;
import io.github.quizup.microservice.core.infrastructure.in.api.response.PageResponse;
import io.github.quizup.microservice.core.infrastructure.mapper.SearchResponseMapper;
import io.github.quizup.theme.domain.model.Question;
import io.github.quizup.theme.infrastructure.in.api.response.QuestionResponse;

import java.util.List;

public final class QuestionResponseMapper {

    private QuestionResponseMapper() {
    }

    public static QuestionResponse toResponse(Question question) {
        return new QuestionResponse(
                question.questionId(),
                question.topicId(),
                question.text(),
                question.imageUrl(),
                question.answers(),
                question.correctAnswer(),
                question.status(),
                question.difficulty(),
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

