package io.github.quizup.theme.infrastructure.out.persistence.adapter;

import io.github.quizup.microservice.core.domain.model.i18n.Language;
import io.github.quizup.microservice.core.infrastructure.adapter.AnnotationSearchableEntity;
import io.github.quizup.microservice.core.infrastructure.adapter.JpaSearchAdapter;
import io.github.quizup.microservice.core.infrastructure.in.api.request.SearchRequest;
import io.github.quizup.microservice.core.infrastructure.in.api.response.SearchResponse;
import io.github.quizup.theme.domain.model.Question;
import io.github.quizup.theme.domain.model.QuestionContent;
import io.github.quizup.theme.domain.model.QuestionStatus;
import io.github.quizup.theme.domain.port.out.QuestionRepositoryPort;
import io.github.quizup.theme.infrastructure.out.persistence.entity.QuestionEntity;
import io.github.quizup.theme.infrastructure.out.persistence.entity.QuestionTranslationEntity;
import io.github.quizup.theme.infrastructure.out.persistence.mapper.QuestionEntityMapper;
import io.github.quizup.theme.infrastructure.out.persistence.repository.QuestionJpaRepository;
import io.github.quizup.theme.infrastructure.out.persistence.repository.QuestionTranslationJpaRepository;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class QuestionRepositoryAdapter implements QuestionRepositoryPort {

    private final QuestionJpaRepository questionJpaRepository;

    private final QuestionTranslationJpaRepository questionTranslationJpaRepository;

    private final JpaSearchAdapter<QuestionEntity> questionJpaSearchAdapter;

    public QuestionRepositoryAdapter(QuestionJpaRepository questionJpaRepository,
                                     QuestionTranslationJpaRepository questionTranslationJpaRepository) {
        this.questionJpaRepository = questionJpaRepository;
        this.questionTranslationJpaRepository = questionTranslationJpaRepository;
        this.questionJpaSearchAdapter = new JpaSearchAdapter<>(questionJpaRepository, new AnnotationSearchableEntity(QuestionEntity.class));
    }

    @Override
    @Transactional
    public void save(Question question) {
        questionJpaRepository.save(QuestionEntityMapper.toEntity(question));

        for (Map.Entry<Language, QuestionContent> translation : question.translations().entrySet()) {
            if (translation.getKey() == question.sourceLanguage()) {
                continue;
            }
            upsertTranslation(question.questionId(), translation.getKey(), translation.getValue());
        }
    }

    private void upsertTranslation(String questionId, Language language, QuestionContent content) {
        QuestionTranslationEntity entity = questionTranslationJpaRepository
                .findByQuestionIdAndLanguage(questionId, language.code())
                .orElseGet(QuestionTranslationEntity::new);

        entity.setQuestionId(questionId);
        entity.setLanguage(language.code());
        entity.setText(content.text());
        entity.getAnswers().clear();
        entity.getAnswers().putAll(content.answers());
        questionTranslationJpaRepository.save(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Question> findById(String questionId) {
        return questionJpaRepository.findById(questionId)
                .map(entity -> QuestionEntityMapper.toDomain(entity,
                        questionTranslationJpaRepository.findByQuestionIdIn(List.of(questionId))));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Question> findByTopicId(String topicId) {
        return withTranslations(questionJpaRepository.findByTopicId(topicId));
    }

    @Override
    @Transactional(readOnly = true)
    public int countApprovedByTopicId(String topicId) {
        return questionJpaRepository.countApprovedByTopicId(topicId);
    }

    @Override
    @Transactional(readOnly = true)
    public int countByTopicIdAndStatus(String topicId, QuestionStatus status) {
        return questionJpaRepository.countByTopicIdAndStatus(topicId, status);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Question> findRandomApprovedByTopicId(String topicId, int count) {
        return withTranslations(questionJpaRepository.findRandomApprovedByTopicId(topicId, count));
    }

    @Override
    @Transactional(readOnly = true)
    public SearchResponse<Question> findAll(SearchRequest request) {
        SearchResponse<QuestionEntity> result = questionJpaSearchAdapter.findAll(request);
        Map<String, List<QuestionTranslationEntity>> byQuestion = loadTranslations(
                result.content().stream().map(QuestionEntity::getQuestionId).toList());

        return result.map(entity -> QuestionEntityMapper.toDomain(entity,
                byQuestion.getOrDefault(entity.getQuestionId(), List.of())));
    }

    private List<Question> withTranslations(List<QuestionEntity> entities) {
        Map<String, List<QuestionTranslationEntity>> byQuestion = loadTranslations(
                entities.stream().map(QuestionEntity::getQuestionId).toList());

        return entities.stream()
                .map(entity -> QuestionEntityMapper.toDomain(entity,
                        byQuestion.getOrDefault(entity.getQuestionId(), List.of())))
                .toList();
    }

    private Map<String, List<QuestionTranslationEntity>> loadTranslations(List<String> questionIds) {
        if (questionIds.isEmpty()) {
            return Map.of();
        }
        return questionTranslationJpaRepository.findByQuestionIdIn(questionIds).stream()
                .collect(Collectors.groupingBy(QuestionTranslationEntity::getQuestionId));
    }
}
