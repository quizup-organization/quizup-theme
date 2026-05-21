package io.github.quizup.theme.domain.port.out;

import io.github.quizup.common.domain.model.search.PageResult;
import io.github.quizup.common.domain.model.search.SearchCriteria;
import io.github.quizup.theme.domain.model.Topic;

import java.util.Optional;

public interface TopicRepositoryPort {
    void save(Topic topic);
    Optional<Topic> findById(String topicId);
    boolean existsById(String topicId);
    PageResult<Topic> findAll(SearchCriteria searchCriteria);
}
