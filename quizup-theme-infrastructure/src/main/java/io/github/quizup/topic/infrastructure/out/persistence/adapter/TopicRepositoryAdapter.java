package io.github.quizup.topic.infrastructure.out.persistence.adapter;

import io.github.quizup.common.domain.model.search.PageResult;
import io.github.quizup.common.domain.model.search.SearchCriteria;
import io.github.quizup.common.infrastructure.adapter.AnnotationSearchableEntity;
import io.github.quizup.common.infrastructure.adapter.JpaSearchAdapter;
import io.github.quizup.topic.domain.model.Topic;
import io.github.quizup.topic.domain.port.out.TopicRepositoryPort;
import io.github.quizup.topic.infrastructure.out.persistence.entity.TopicEntity;
import io.github.quizup.topic.infrastructure.out.persistence.mapper.TopicEntityMapper;
import io.github.quizup.topic.infrastructure.out.persistence.repository.TopicJpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Component
public class TopicRepositoryAdapter implements TopicRepositoryPort {

    private final TopicJpaRepository topicJpaRepository;
    private final JpaSearchAdapter<TopicEntity> topicJpaSearchAdapter;

    public TopicRepositoryAdapter(TopicJpaRepository topicJpaRepository) {
        this.topicJpaRepository = topicJpaRepository;
        this.topicJpaSearchAdapter = new JpaSearchAdapter<>(topicJpaRepository, new AnnotationSearchableEntity(TopicEntity.class));
    }

    @Override
    @Transactional
    public void save(Topic topic) {
        topicJpaRepository.save(TopicEntityMapper.toEntity(topic));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Topic> findById(String topicId) {
        return topicJpaRepository.findById(topicId)
                .map(TopicEntityMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsById(String topicId) {
        return topicJpaRepository.existsById(topicId);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<Topic> findAll(SearchCriteria searchCriteria) {
        return topicJpaSearchAdapter.findAll(searchCriteria)
                .map(TopicEntityMapper::toDomain);
    }
}

