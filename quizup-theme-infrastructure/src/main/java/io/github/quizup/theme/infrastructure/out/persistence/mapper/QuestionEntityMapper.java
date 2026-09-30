package io.github.quizup.theme.infrastructure.out.persistence.mapper;

import io.github.quizup.microservice.core.domain.model.i18n.Language;
import io.github.quizup.theme.domain.model.Question;
import io.github.quizup.theme.domain.model.QuestionContent;
import io.github.quizup.theme.infrastructure.out.persistence.entity.QuestionContentEntity;
import io.github.quizup.theme.infrastructure.out.persistence.entity.QuestionEntity;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class QuestionEntityMapper {

    private QuestionEntityMapper() {
    }

    /**
     * Convertit l'entité (métadonnées) + ses contenus localisés en modèle domaine.
     */
    public static Question toDomain(QuestionEntity entity, List<QuestionContentEntity> contentEntities) {
        Map<Language, QuestionContent> contents = new EnumMap<>(Language.class);
        for (QuestionContentEntity contentEntity : contentEntities) {
            Language language = Language.fromCode(contentEntity.getLanguage());
            contents.put(language, new QuestionContent(
                    language,
                    contentEntity.getText(),
                    new HashMap<>(contentEntity.getAnswers())));
        }

        return Question.builder()
                .questionId(entity.getQuestionId())
                .topicId(entity.getTopicId())
                .contents(contents)
                .imageUrl(entity.getImageUrl())
                .correctAnswer(entity.getCorrectAnswer())
                .status(entity.getStatus())
                .difficulty(entity.getDifficulty())
                .creatorId(entity.getCreatorId())
                .updatedBy(entity.getUpdatedBy())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    /**
     * Convertit le modèle domaine en entité de métadonnées.
     */
    public static QuestionEntity toEntity(Question question) {
        QuestionEntity entity = new QuestionEntity();
        entity.setQuestionId(question.questionId());
        entity.setTopicId(question.topicId());
        entity.setImageUrl(question.imageUrl());
        entity.setCorrectAnswer(question.correctAnswer());
        entity.setStatus(question.status());
        entity.setDifficulty(question.difficulty());
        entity.setCreatorId(question.creatorId());
        entity.setUpdatedBy(question.updatedBy());
        entity.setCreatedAt(question.createdAt());
        entity.setUpdatedAt(question.updatedAt());
        return entity;
    }

    public static QuestionContentEntity toContentEntity(String questionId, QuestionContent content) {
        QuestionContentEntity entity = new QuestionContentEntity();
        entity.setQuestionId(questionId);
        entity.setLanguage(content.language().code());
        entity.setText(content.text());
        entity.getAnswers().clear();
        entity.getAnswers().putAll(content.answers());
        return entity;
    }
}
