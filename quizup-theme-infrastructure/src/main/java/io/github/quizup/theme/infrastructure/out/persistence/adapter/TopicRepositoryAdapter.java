package io.github.quizup.theme.infrastructure.out.persistence.adapter;

import io.github.quizup.microservice.core.infrastructure.in.api.request.SearchRequest;
import io.github.quizup.microservice.core.infrastructure.in.api.response.SearchResponse;
import io.github.quizup.microservice.core.infrastructure.adapter.AnnotationSearchableEntity;
import io.github.quizup.microservice.core.infrastructure.adapter.JpaSearchAdapter;
import io.github.quizup.theme.domain.model.Topic;
import io.github.quizup.theme.domain.port.out.TopicRepositoryPort;
import io.github.quizup.theme.infrastructure.out.persistence.entity.TopicEntity;
import io.github.quizup.theme.infrastructure.out.persistence.mapper.TopicEntityMapper;
import io.github.quizup.theme.infrastructure.out.persistence.repository.TopicJpaRepository;
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
    public SearchResponse<Topic> findAll(SearchRequest request) {
        return topicJpaSearchAdapter.findAll(request)
                .map(TopicEntityMapper::toDomain);
    }
}
