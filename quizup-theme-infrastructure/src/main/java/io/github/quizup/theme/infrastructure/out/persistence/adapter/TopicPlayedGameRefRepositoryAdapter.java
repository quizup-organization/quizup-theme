package io.github.quizup.theme.infrastructure.out.persistence.adapter;

import io.github.quizup.theme.domain.port.out.TopicPlayedGameRefRepositoryPort;
import io.github.quizup.theme.infrastructure.out.persistence.entity.TopicPlayedGameRefEntity;
import io.github.quizup.theme.infrastructure.out.persistence.repository.TopicPlayedGameRefJpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class TopicPlayedGameRefRepositoryAdapter implements TopicPlayedGameRefRepositoryPort {

    private final TopicPlayedGameRefJpaRepository repository;

    public TopicPlayedGameRefRepositoryAdapter(TopicPlayedGameRefJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public void record(String gameId, String topicId) {
        if (repository.existsById(gameId)) {
            return;
        }
        repository.save(new TopicPlayedGameRefEntity(gameId, topicId));
    }

    @Override
    @Transactional(readOnly = true)
    public int countByTopicId(String topicId) {
        return repository.countByTopicId(topicId);
    }
}
