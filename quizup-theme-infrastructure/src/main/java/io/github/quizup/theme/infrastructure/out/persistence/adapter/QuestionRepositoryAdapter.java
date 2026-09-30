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
import io.github.quizup.theme.infrastructure.out.persistence.entity.QuestionContentEntity;
import io.github.quizup.theme.infrastructure.out.persistence.entity.QuestionEntity;
import io.github.quizup.theme.infrastructure.out.persistence.mapper.QuestionEntityMapper;
import io.github.quizup.theme.infrastructure.out.persistence.repository.QuestionContentJpaRepository;
import io.github.quizup.theme.infrastructure.out.persistence.repository.QuestionJpaRepository;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class QuestionRepositoryAdapter implements QuestionRepositoryPort {

    private final QuestionJpaRepository questionJpaRepository;

    private final QuestionContentJpaRepository questionContentJpaRepository;

    private final JpaSearchAdapter<QuestionEntity> questionJpaSearchAdapter;

    public QuestionRepositoryAdapter(QuestionJpaRepository questionJpaRepository,
                                     QuestionContentJpaRepository questionContentJpaRepository) {
        this.questionJpaRepository = questionJpaRepository;
        this.questionContentJpaRepository = questionContentJpaRepository;
        this.questionJpaSearchAdapter = new JpaSearchAdapter<>(questionJpaRepository, new AnnotationSearchableEntity(QuestionEntity.class));
    }

    @Override
    @Transactional
    public void save(Question question) {
        questionJpaRepository.save(QuestionEntityMapper.toEntity(question));

        for (Map.Entry<Language, QuestionContent> content : question.contents().entrySet()) {
            upsertContent(question.questionId(), content.getValue());
        }
    }

    private void upsertContent(String questionId, QuestionContent content) {
        QuestionContentEntity entity = questionContentJpaRepository
                .findByQuestionIdAndLanguage(questionId, content.language().code())
                .orElseGet(QuestionContentEntity::new);

        entity.setQuestionId(questionId);
        entity.setLanguage(content.language().code());
        entity.setText(content.text());
        entity.getAnswers().clear();
        entity.getAnswers().putAll(content.answers());
        questionContentJpaRepository.save(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Question> findById(String questionId) {
        return questionJpaRepository.findById(questionId)
                .map(entity -> QuestionEntityMapper.toDomain(entity,
                        questionContentJpaRepository.findByQuestionIdIn(List.of(questionId))));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Question> findByTopicId(String topicId) {
        return withContents(questionJpaRepository.findByTopicId(topicId));
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
    public int countApprovedByTopicAndLanguages(String topicId, Set<Language> languages) {
        if (languages == null || languages.isEmpty()) {
            return 0;
        }
        List<String> languageCodes = languages.stream().map(Language::code).toList();
        return questionJpaRepository.countApprovedByTopicAndLanguages(topicId, languageCodes, languages.size());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Question> findRandomApprovedByTopicId(String topicId, int count, Set<Language> languages) {
        if (languages == null || languages.isEmpty()) {
            return List.of();
        }
        List<String> languageCodes = languages.stream().map(Language::code).toList();
        return withContents(questionJpaRepository.findRandomApprovedByTopicId(
                topicId, count, languageCodes, languages.size()));
    }

    @Override
    @Transactional(readOnly = true)
    public SearchResponse<Question> findAll(SearchRequest request) {
        SearchResponse<QuestionEntity> result = questionJpaSearchAdapter.findAll(request);
        Map<String, List<QuestionContentEntity>> byQuestion = loadContents(
                result.content().stream().map(QuestionEntity::getQuestionId).toList());

        return result.map(entity -> QuestionEntityMapper.toDomain(entity,
                byQuestion.getOrDefault(entity.getQuestionId(), List.of())));
    }

    private List<Question> withContents(List<QuestionEntity> entities) {
        Map<String, List<QuestionContentEntity>> byQuestion = loadContents(
                entities.stream().map(QuestionEntity::getQuestionId).toList());

        return entities.stream()
                .map(entity -> QuestionEntityMapper.toDomain(entity,
                        byQuestion.getOrDefault(entity.getQuestionId(), List.of())))
                .toList();
    }

    private Map<String, List<QuestionContentEntity>> loadContents(List<String> questionIds) {
        if (questionIds.isEmpty()) {
            return Map.of();
        }
        return questionContentJpaRepository.findByQuestionIdIn(questionIds).stream()
                .collect(Collectors.groupingBy(QuestionContentEntity::getQuestionId));
    }
}
