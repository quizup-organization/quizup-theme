package io.github.quizup.theme.domain.port.out;

import io.github.quizup.microservice.core.infrastructure.in.api.request.SearchRequest;
import io.github.quizup.microservice.core.infrastructure.in.api.response.SearchResponse;
import io.github.quizup.theme.domain.model.Question;
import io.github.quizup.theme.domain.model.QuestionStatus;

import java.util.List;
import java.util.Optional;

public interface QuestionRepositoryPort {

    void save(Question question);

    Optional<Question> findById(String questionId);

    int countApprovedByTopicId(String topicId);

    int countByTopicIdAndStatus(String topicId, QuestionStatus status);

    List<Question> findRandomApprovedByTopicId(String topicId, int count);

    SearchResponse<Question> findAll(SearchRequest request);
}
