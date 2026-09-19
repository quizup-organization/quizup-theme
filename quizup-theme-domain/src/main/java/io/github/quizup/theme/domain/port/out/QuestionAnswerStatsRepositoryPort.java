package io.github.quizup.theme.domain.port.out;

import io.github.quizup.theme.domain.model.QuestionAnswerStats;

import java.util.Optional;

public interface QuestionAnswerStatsRepositoryPort {

    Optional<QuestionAnswerStats> findById(String questionId);

    void save(QuestionAnswerStats stats);
}
