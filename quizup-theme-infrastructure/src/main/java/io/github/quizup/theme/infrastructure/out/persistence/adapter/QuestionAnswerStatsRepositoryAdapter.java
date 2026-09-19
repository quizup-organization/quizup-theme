package io.github.quizup.theme.infrastructure.out.persistence.adapter;

import io.github.quizup.theme.domain.model.QuestionAnswerStats;
import io.github.quizup.theme.domain.port.out.QuestionAnswerStatsRepositoryPort;
import io.github.quizup.theme.infrastructure.out.persistence.mapper.QuestionAnswerStatsEntityMapper;
import io.github.quizup.theme.infrastructure.out.persistence.repository.QuestionAnswerStatsJpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Component
public class QuestionAnswerStatsRepositoryAdapter implements QuestionAnswerStatsRepositoryPort {

    private final QuestionAnswerStatsJpaRepository repository;

    public QuestionAnswerStatsRepositoryAdapter(QuestionAnswerStatsJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<QuestionAnswerStats> findById(String questionId) {
        return repository.findById(questionId).map(QuestionAnswerStatsEntityMapper::toDomain);
    }

    @Override
    @Transactional
    public void save(QuestionAnswerStats stats) {
        repository.save(QuestionAnswerStatsEntityMapper.toEntity(stats));
    }
}
