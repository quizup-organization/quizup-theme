package io.github.quizup.theme.infrastructure.out.persistence.adapter;

import io.github.quizup.microservice.core.infrastructure.in.api.request.SearchRequest;
import io.github.quizup.microservice.core.infrastructure.in.api.response.SearchResponse;
import io.github.quizup.microservice.core.infrastructure.adapter.AnnotationSearchableEntity;
import io.github.quizup.microservice.core.infrastructure.adapter.JpaSearchAdapter;
import io.github.quizup.theme.domain.model.Topic;
import io.github.quizup.theme.domain.model.TopicCategory;
import io.github.quizup.theme.domain.model.TopicFacetCount;
import io.github.quizup.theme.domain.model.TopicPage;
import io.github.quizup.theme.domain.model.TopicSort;
import io.github.quizup.theme.domain.model.TopicStatus;
import io.github.quizup.theme.domain.port.out.TopicRepositoryPort;
import io.github.quizup.theme.infrastructure.out.persistence.entity.TopicEntity;
import io.github.quizup.theme.infrastructure.out.persistence.mapper.TopicEntityMapper;
import io.github.quizup.theme.infrastructure.out.persistence.repository.TopicJpaRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class TopicRepositoryAdapter implements TopicRepositoryPort {

    private final TopicJpaRepository topicJpaRepository;
    private final JpaSearchAdapter<TopicEntity> topicJpaSearchAdapter;

    public TopicRepositoryAdapter(TopicJpaRepository topicJpaRepository) {
        this.topicJpaRepository = topicJpaRepository;
        this.topicJpaSearchAdapter = new JpaSearchAdapter<>(topicJpaRepository, new AnnotationSearchableEntity(TopicEntity.class));
    }

    @Override
    @Transactional
    public void save(Topic topic) {
        topicJpaRepository.save(TopicEntityMapper.toEntity(topic));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Topic> findById(String topicId) {
        return topicJpaRepository.findById(topicId)
                .map(TopicEntityMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsById(String topicId) {
        return topicJpaRepository.existsById(topicId);
    }

    @Override
    @Transactional(readOnly = true)
    public SearchResponse<Topic> findAll(SearchRequest request) {
        return topicJpaSearchAdapter.findAll(request)
                .map(TopicEntityMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TopicFacetCount> countByCategory(String normalizedName, List<String> topicIds) {
        if (topicIds != null && topicIds.isEmpty()) {
            return List.of();
        }
        List<Object[]> rows = topicIds == null
                ? topicJpaRepository.countByCategory(TopicStatus.PUBLISHED, normalizedName)
                : topicJpaRepository.countByCategoryInIds(TopicStatus.PUBLISHED, normalizedName, topicIds);
        return rows.stream()
                .map(row -> new TopicFacetCount((TopicCategory) row[0], ((Number) row[1]).longValue()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public TopicPage findPage(String normalizedName, TopicCategory category, TopicSort sort, int page, int size) {
        Page<TopicEntity> result = topicJpaRepository.findAll(
                publishedFilter(normalizedName, category),
                PageRequest.of(page, size, sortFor(sort)));
        return TopicPage.builder()
                .topics(result.getContent().stream().map(TopicEntityMapper::toDomain).toList())
                .page(result.getNumber())
                .size(result.getSize())
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public TopicPage findByCreatorId(String creatorId, int page, int size) {
        Page<TopicEntity> result = topicJpaRepository.findByCreatorIdOrderByUpdatedAtDesc(
                creatorId, PageRequest.of(page, size));
        return TopicPage.builder()
                .topics(result.getContent().stream().map(TopicEntityMapper::toDomain).toList())
                .page(result.getNumber())
                .size(result.getSize())
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .build();
    }

    private Specification<TopicEntity> publishedFilter(String normalizedName, TopicCategory category) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("status"), TopicStatus.PUBLISHED));
            if (normalizedName != null) {
                predicates.add(cb.like(root.get("nameNormalized"), "%" + normalizedName + "%"));
            }
            if (category != null) {
                predicates.add(cb.equal(root.get("category"), category));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    private Sort sortFor(TopicSort sort) {
        return sort == TopicSort.ALPHA
                ? Sort.by("name").ascending()
                : Sort.by(Sort.Order.desc("followersCounter"), Sort.Order.asc("name"));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Topic> findAllByIds(List<String> topicIds) {
        if (topicIds.isEmpty()) {
            return List.of();
        }
        Map<String, Topic> byId = topicJpaRepository.findAllById(topicIds).stream()
                .map(TopicEntityMapper::toDomain)
                .collect(Collectors.toMap(Topic::topicId, Function.identity(), (first, _) -> first));
        return topicIds.stream()
                .map(byId::get)
                .filter(Objects::nonNull)
                .toList();
    }
}
