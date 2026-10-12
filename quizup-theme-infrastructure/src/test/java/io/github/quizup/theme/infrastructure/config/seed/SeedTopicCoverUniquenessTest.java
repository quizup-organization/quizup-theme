package io.github.quizup.theme.infrastructure.config.seed;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Garde-fou de contenu : deux thèmes du seed ne partagent jamais la même couverture
 * ({@code topic.imageUrl}) — le catalogue affiche ces visuels côte à côte, un doublon saute aux
 * yeux (constat prod : Alnwick Castle sur 4 films Harry Potter, drapeau ONU sur deux thèmes…).
 * Les thèmes sans couverture utilisent le fallback UI et sont hors périmètre.
 */
class SeedTopicCoverUniquenessTest {

    @Test
    void everySeedTopicHasDistinctCoverImage() {
        SeedDataLoader loader = new SeedDataLoader(
                "classpath*:seed/topics/*/*.yml", new PathMatchingResourcePatternResolver());

        List<TopicSeedDefinition> topics = loader.loadAll();
        assertThat(topics).hasSizeGreaterThanOrEqualTo(20);

        Map<String, String> topicByImage = new HashMap<>();
        List<String> duplicates = new ArrayList<>();
        for (TopicSeedDefinition topic : topics) {
            String image = topic.imageUrl();
            if (image == null || image.isBlank()) {
                continue;
            }
            String previous = topicByImage.putIfAbsent(image, topic.topicId());
            if (previous != null) {
                duplicates.add(previous + " + " + topic.topicId() + " -> " + image);
            }
        }

        assertThat(duplicates)
                .as("couvertures de thème dupliquées dans le seed")
                .isEmpty();
    }
}
