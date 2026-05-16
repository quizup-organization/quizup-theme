package io.github.quizup.topic.infrastructure.out.persistence.adapter;

import io.github.quizup.common.domain.model.search.PageResult;
import io.github.quizup.common.domain.model.search.SearchCriteria;
import io.github.quizup.common.infrastructure.adapter.AnnotationSearchableEntity;
import io.github.quizup.common.infrastructure.adapter.JpaSearchAdapter;
import io.github.quizup.topic.domain.model.Question;
import io.github.quizup.topic.domain.port.out.QuestionRepositoryPort;
import io.github.quizup.topic.infrastructure.out.persistence.entity.QuestionEntity;
import io.github.quizup.topic.infrastructure.out.persistence.mapper.QuestionEntityMapper;
import io.github.quizup.topic.infrastructure.out.persistence.repository.QuestionJpaRepository;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Component
public class QuestionRepositoryAdapter implements QuestionRepositoryPort {

    private final QuestionJpaRepository questionJpaRepository;

    private final JpaSearchAdapter<QuestionEntity> questionJpaSearchAdapter;

    public QuestionRepositoryAdapter(QuestionJpaRepository questionJpaRepository) {
        this.questionJpaRepository = questionJpaRepository;
        this.questionJpaSearchAdapter = new JpaSearchAdapter<>(questionJpaRepository, new AnnotationSearchableEntity(QuestionEntity.class));
    }

    @Override
    @Transactional
    public void save(Question question) {
        questionJpaRepository.save(QuestionEntityMapper.toEntity(question));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Question> findById(String questionId) {
        return questionJpaRepository.findById(questionId).map(QuestionEntityMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public int countApprovedByTopicId(String topicId) {
        return questionJpaRepository.countApprovedByTopicId(topicId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Question> findRandomApprovedByTopicId(String topicId, int count) {
        return questionJpaRepository.findRandomApprovedByTopicId(topicId, count)
                .stream()
                .map(QuestionEntityMapper::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<Question> findAll(SearchCriteria searchCriteria) {
        return questionJpaSearchAdapter.findAll(searchCriteria, QuestionEntityMapper::toDomain);
    }
}

