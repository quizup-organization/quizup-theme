package io.github.quizup.theme.domain.port.out;

import io.github.quizup.common.domain.model.search.PageResult;
import io.github.quizup.common.domain.model.search.SearchCriteria;
import io.github.quizup.theme.domain.model.Question;

import java.util.List;
import java.util.Optional;

public interface QuestionRepositoryPort {

    void save(Question question);

    Optional<Question> findById(String questionId);

    int countApprovedByTopicId(String topicId);

    List<Question> findRandomApprovedByTopicId(String topicId, int count);

    PageResult<Question> findAll(SearchCriteria searchCriteria);
}
