package io.github.quizup.theme.infrastructure.out.persistence.adapter;

import io.github.quizup.microservice.core.domain.model.i18n.Language;
import io.github.quizup.microservice.core.infrastructure.adapter.AnnotationSearchableEntity;
import io.github.quizup.microservice.core.infrastructure.adapter.JpaSearchAdapter;
import io.github.quizup.microservice.core.infrastructure.in.api.request.SearchRequest;
import io.github.quizup.microservice.core.infrastructure.in.api.response.SearchResponse;
import io.github.quizup.theme.domain.model.Question;
import io.github.quizup.theme.domain.model.QuestionAdminFilter;
import io.github.quizup.theme.domain.model.QuestionAdminPage;
import io.github.quizup.theme.domain.model.QuestionAdminSort;
import io.github.quizup.theme.domain.model.QuestionContent;
import io.github.quizup.theme.domain.model.QuestionDifficulty;
import io.github.quizup.theme.domain.model.QuestionModerationFilter;
import io.github.quizup.theme.domain.model.QuestionReportStatus;
import io.github.quizup.theme.domain.model.QuestionStatus;
import io.github.quizup.theme.domain.port.out.QuestionRepositoryPort;
import io.github.quizup.theme.infrastructure.out.persistence.entity.QuestionContentEntity;
import io.github.quizup.theme.infrastructure.out.persistence.entity.QuestionEntity;
import io.github.quizup.theme.infrastructure.out.persistence.entity.QuestionReportEntity;
import io.github.quizup.theme.infrastructure.out.persistence.mapper.QuestionEntityMapper;
import io.github.quizup.theme.infrastructure.out.persistence.repository.QuestionContentJpaRepository;
import io.github.quizup.theme.infrastructure.out.persistence.repository.QuestionJpaRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
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
    public List<Question> findByIds(List<String> questionIds) {
        if (questionIds == null || questionIds.isEmpty()) {
            return List.of();
        }
        Map<String, Question> byId = withContents(questionJpaRepository.findAllById(questionIds)).stream()
                .collect(Collectors.toMap(Question::questionId, question -> question, (first, _) -> first));
        return questionIds.stream()
                .map(byId::get)
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public QuestionAdminPage findAdminPage(QuestionAdminFilter filter,
                                           QuestionAdminSort sort,
                                           int page,
                                           int size) {
        Page<QuestionEntity> result = questionJpaRepository.findAll(
                adminFilter(filter),
                PageRequest.of(page, size, adminSort(sort)));
        return new QuestionAdminPage(
                withContents(result.getContent()),
                result.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public int countSuspended() {
        return questionJpaRepository.countSuspended();
    }

    @Override
    @Transactional(readOnly = true)
    public int countArchived() {
        return questionJpaRepository.countArchived();
    }

    private static Specification<QuestionEntity> adminFilter(QuestionAdminFilter filter) {
        QuestionAdminFilter effective = filter == null
                ? new QuestionAdminFilter(null, null, null, null, QuestionModerationFilter.ALL, null)
                : filter;
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (effective.text() != null && !effective.text().isBlank()) {
                String pattern = "%" + effective.text().toLowerCase(Locale.ROOT) + "%";
                Subquery<String> textSubquery = query.subquery(String.class);
                Root<QuestionContentEntity> content = textSubquery.from(QuestionContentEntity.class);
                textSubquery.select(content.get("questionId"))
                        .where(cb.like(cb.lower(content.get("text")), pattern));
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("questionId")), pattern),
                        root.get("questionId").in(textSubquery)));
            }
            if (effective.topicId() != null && !effective.topicId().isBlank()) {
                predicates.add(cb.equal(root.get("topicId"), effective.topicId()));
            }
            if (effective.status() != null) {
                predicates.add(cb.equal(root.get("status"), effective.status()));
            }
            if (effective.difficulty() != null) {
                predicates.add(cb.equal(root.get("difficulty"), effective.difficulty()));
            }
            QuestionModerationFilter moderation = effective.moderation() == null
                    ? QuestionModerationFilter.ALL
                    : effective.moderation();
            switch (moderation) {
                case ACTIVE -> {
                    predicates.add(cb.isFalse(root.<Boolean>get("suspended")));
                    predicates.add(cb.isFalse(root.<Boolean>get("archived")));
                }
                case SUSPENDED -> {
                    predicates.add(cb.isTrue(root.<Boolean>get("suspended")));
                    predicates.add(cb.isFalse(root.<Boolean>get("archived")));
                }
                case ARCHIVED -> predicates.add(cb.isTrue(root.<Boolean>get("archived")));
                case ALL -> predicates.add(cb.isFalse(root.<Boolean>get("archived")));
            }
            if (Boolean.TRUE.equals(effective.reportedOnly())) {
                Subquery<String> reportSubquery = query.subquery(String.class);
                Root<QuestionReportEntity> report = reportSubquery.from(QuestionReportEntity.class);
                reportSubquery.select(report.get("questionId"))
                        .where(cb.equal(report.get("status"), QuestionReportStatus.OPEN));
                predicates.add(root.get("questionId").in(reportSubquery));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    private static Sort adminSort(QuestionAdminSort sort) {
        if (sort == QuestionAdminSort.OLDEST_UPDATED) {
            return Sort.by(Sort.Order.asc("updatedAt"), Sort.Order.asc("questionId"));
        }
        return Sort.by(Sort.Order.desc("updatedAt"), Sort.Order.asc("questionId"));
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
    public List<Question> findRandomApprovedByTopicId(String topicId, int count, Set<Language> languages,
                                                      Set<QuestionDifficulty> difficulties,
                                                      Set<String> excludeQuestionIds) {
        if (languages == null || languages.isEmpty()) {
            return List.of();
        }
        List<String> languageCodes = languages.stream().map(Language::code).toList();
        // Sur-échantillonnage borné : le dédoublonnage par contenu (texte + image) doit pouvoir
        // rendre malgré tout `count` questions distinctes.
        int oversample = count + Math.max(4, count);
        List<QuestionEntity> entities = questionJpaRepository.findRandomApprovedByTopicId(
                topicId,
                oversample,
                languageCodes,
                languages.size(),
                csv(excludeQuestionIds),
                csv(difficulties == null
                        ? Set.of()
                        : difficulties.stream().map(Enum::name).collect(Collectors.toSet())));

        return distinctByContent(withContents(entities), count);
    }

    /**
     * Dédoublonne le tirage par **contenu joué** (texte + illustration) en conservant l'ordre
     * aléatoire. Le read model peut porter deux lignes identiques pour un même thème (re-seed dont
     * l'URL d'image résolue a changé, ancienne question laissée approuvée) : sans ce filtre, une
     * même partie pourrait proposer deux fois la même question.
     */
    private static List<Question> distinctByContent(List<Question> questions, int count) {
        Map<String, Question> unique = new LinkedHashMap<>();
        for (Question question : questions) {
            unique.putIfAbsent(contentKey(question), question);
        }
        return unique.values().stream().limit(count).toList();
    }

    /** Clé de contenu d'une question : textes localisés (langue indifférente) + illustration. */
    private static String contentKey(Question question) {
        String text = question.contents() == null
                ? ""
                : question.contents().values().stream()
                        .map(content -> content.text() == null ? "" : content.text())
                        .sorted()
                        .findFirst()
                        .orElse("");
        return text + "\u0000" + (question.imageUrl() == null ? "" : question.imageUrl());
    }

    /** Préférences sérialisées en CSV (vide = neutre) pour le tri best-effort du tirage. */
    private static String csv(Set<String> values) {
        return values == null || values.isEmpty() ? "" : String.join(",", values);
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
