package io.github.quizup.theme.infrastructure.out.persistence.adapter;

import io.github.quizup.theme.domain.port.out.QuestionAnswerRecordPort;
import io.github.quizup.theme.infrastructure.out.persistence.entity.QuestionAnswerRecordEntity;
import io.github.quizup.theme.infrastructure.out.persistence.entity.QuestionAnswerRecordId;
import io.github.quizup.theme.infrastructure.out.persistence.repository.QuestionAnswerRecordJpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Component
public class QuestionAnswerRecordAdapter implements QuestionAnswerRecordPort {

    private final QuestionAnswerRecordJpaRepository repository;

    public QuestionAnswerRecordAdapter(QuestionAnswerRecordJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public boolean record(String gameId, String round, String playerId, String questionId, boolean correct) {
        QuestionAnswerRecordId id = new QuestionAnswerRecordId(gameId, round, playerId);
        if (repository.existsById(id)) {
            return false;
        }
        repository.save(new QuestionAnswerRecordEntity(id, questionId, correct, Instant.now()));
        return true;
    }
}
