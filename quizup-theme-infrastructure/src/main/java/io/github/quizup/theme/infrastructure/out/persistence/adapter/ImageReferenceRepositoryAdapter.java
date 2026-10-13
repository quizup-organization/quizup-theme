package io.github.quizup.theme.infrastructure.out.persistence.adapter;

import io.github.quizup.theme.domain.model.ImageReferenceCount;
import io.github.quizup.theme.domain.port.out.ImageReferenceRepositoryPort;
import io.github.quizup.theme.infrastructure.out.persistence.repository.QuestionJpaRepository;
import io.github.quizup.theme.infrastructure.out.persistence.repository.TopicJpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class ImageReferenceRepositoryAdapter implements ImageReferenceRepositoryPort {

    private final TopicJpaRepository topicJpaRepository;
    private final QuestionJpaRepository questionJpaRepository;

    public ImageReferenceRepositoryAdapter(TopicJpaRepository topicJpaRepository,
                                           QuestionJpaRepository questionJpaRepository) {
        this.topicJpaRepository = topicJpaRepository;
        this.questionJpaRepository = questionJpaRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ImageReferenceCount> countByImageUrls(List<String> imageUrls) {
        if (imageUrls == null || imageUrls.isEmpty()) {
            return List.of();
        }
        Map<String, Long> counts = new HashMap<>();
        addCounts(counts, topicJpaRepository.countByImageUrlIn(imageUrls));
        addCounts(counts, topicJpaRepository.countByBannerUrlIn(imageUrls));
        addCounts(counts, questionJpaRepository.countByImageUrlIn(imageUrls));
        return imageUrls.stream()
                .distinct()
                .filter(counts::containsKey)
                .map(url -> new ImageReferenceCount(url, counts.get(url)))
                .toList();
    }

    private static void addCounts(Map<String, Long> counts, List<Object[]> rows) {
        for (Object[] row : rows) {
            counts.merge((String) row[0], ((Number) row[1]).longValue(), Long::sum);
        }
    }
}
