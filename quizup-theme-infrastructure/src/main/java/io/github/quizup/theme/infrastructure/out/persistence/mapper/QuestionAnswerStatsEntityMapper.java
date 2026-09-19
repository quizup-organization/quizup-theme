package io.github.quizup.theme.infrastructure.out.persistence.mapper;

import io.github.quizup.theme.domain.model.QuestionAnswerStats;
import io.github.quizup.theme.infrastructure.out.persistence.entity.QuestionAnswerStatsEntity;

public final class QuestionAnswerStatsEntityMapper {

    private QuestionAnswerStatsEntityMapper() {
    }

    public static QuestionAnswerStats toDomain(QuestionAnswerStatsEntity entity) {
        return new QuestionAnswerStats(
                entity.getQuestionId(),
                entity.getAnswerCount(),
                entity.getCorrectCount()
        );
    }

    public static QuestionAnswerStatsEntity toEntity(QuestionAnswerStats stats) {
        QuestionAnswerStatsEntity entity = new QuestionAnswerStatsEntity();
        entity.setQuestionId(stats.questionId());
        entity.setAnswerCount(stats.answerCount());
        entity.setCorrectCount(stats.correctCount());
        return entity;
    }
}
