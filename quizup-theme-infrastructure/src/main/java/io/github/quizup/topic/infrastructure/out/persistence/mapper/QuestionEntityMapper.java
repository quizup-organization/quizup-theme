package io.github.quizup.topic.infrastructure.out.persistence.mapper;

import io.github.quizup.topic.domain.model.Question;
import io.github.quizup.topic.infrastructure.out.persistence.entity.QuestionEntity;

public final class QuestionEntityMapper {

    private QuestionEntityMapper() {
    }

    public static Question toDomain(QuestionEntity entity) {
        return new Question(
                entity.getQuestionId(),
                entity.getTopicId(),
                entity.getText(),
                entity.getAnswers(),
                entity.getCorrectAnswer(),
                entity.getStatus(),
                entity.getCreatorId(),
                entity.getUpdatedBy(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public static QuestionEntity toEntity(Question question) {
        QuestionEntity questionEntity = new QuestionEntity();
        questionEntity.setQuestionId(question.questionId());
        questionEntity.setTopicId(question.topicId());
        questionEntity.setText(question.text());
        questionEntity.setAnswers(question.answers());
        questionEntity.setCorrectAnswer(question.correctAnswer());
        questionEntity.setStatus(question.status());
        questionEntity.setCreatorId(question.creatorId());
        questionEntity.setUpdatedBy(question.updatedBy());
        questionEntity.setCreatedAt(question.createdAt());
        questionEntity.setUpdatedAt(question.updatedAt());
        return questionEntity;
    }
}

