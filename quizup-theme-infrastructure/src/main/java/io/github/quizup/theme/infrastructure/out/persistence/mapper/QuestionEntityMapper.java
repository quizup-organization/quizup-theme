package io.github.quizup.theme.infrastructure.out.persistence.mapper;

import io.github.quizup.microservice.core.domain.model.i18n.Language;
import io.github.quizup.theme.domain.model.Question;
import io.github.quizup.theme.domain.model.QuestionContent;
import io.github.quizup.theme.infrastructure.out.persistence.entity.QuestionEntity;
import io.github.quizup.theme.infrastructure.out.persistence.entity.QuestionTranslationEntity;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class QuestionEntityMapper {

    private QuestionEntityMapper() {
    }

    /**
     * Convertit l'entité source (+ ses traductions, hors langue source) en modèle domaine.
     */
    public static Question toDomain(QuestionEntity entity, List<QuestionTranslationEntity> translationEntities) {
        Language sourceLanguage = Language.fromCode(entity.getSourceLanguage());

        Map<Language, QuestionContent> translations = new EnumMap<>(Language.class);
        translations.put(sourceLanguage,
                new QuestionContent(entity.getText(), new HashMap<>(entity.getAnswers())));
        for (QuestionTranslationEntity translation : translationEntities) {
            Language language = Language.fromCode(translation.getLanguage());
            translations.put(language,
                    new QuestionContent(translation.getText(), new HashMap<>(translation.getAnswers())));
        }

        return Question.builder()
                .questionId(entity.getQuestionId())
                .topicId(entity.getTopicId())
                .sourceLanguage(sourceLanguage)
                .translations(translations)
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
     * Convertit le modèle domaine en entité source (texte/réponses de la langue source).
     */
    public static QuestionEntity toEntity(Question question) {
        QuestionContent source = question.content(question.sourceLanguage());

        QuestionEntity entity = new QuestionEntity();
        entity.setQuestionId(question.questionId());
        entity.setTopicId(question.topicId());
        entity.setSourceLanguage(question.sourceLanguage().code());
        entity.setText(source.text());
        entity.setAnswers(new HashMap<>(source.answers()));
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

    public static QuestionTranslationEntity toTranslationEntity(String questionId,
                                                                Language language,
                                                                QuestionContent content) {
        QuestionTranslationEntity entity = new QuestionTranslationEntity();
        entity.setQuestionId(questionId);
        entity.setLanguage(language.code());
        entity.setText(content.text());
        entity.getAnswers().clear();
        entity.getAnswers().putAll(content.answers());
        return entity;
    }
}
