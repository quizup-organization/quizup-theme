package io.github.quizup.theme.domain.model;

import lombok.Builder;

import java.util.List;

/**
 * Page du catalogue de sujets (query dédiée, sans {@code SearchRequest}).
 */
@Builder(toBuilder = true)
public record TopicPage(
        List<Topic> topics,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}
