package io.github.quizup.theme.infrastructure.config.seed;

import io.github.quizup.theme.domain.model.TopicTitles;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Garde-fou de contenu : les titres personnalisés du seed réel ({@code topic.titles}) sont
 * **optionnels**, mais lorsqu'ils sont présents ils sont bien formés : grades connus, libellés
 * non vides et bornés (le repli « grade · nom du sujet » couvre les manques).
 */
class SeedTopicTitlesTest {

    @Test
    void seedTitlesAreWellFormedWhenPresent() {
        SeedDataLoader loader = new SeedDataLoader(
                "classpath*:seed/topics/*/*.yml", "https://media.example.test/quizup-media/",
                new PathMatchingResourcePatternResolver());

        List<TopicSeedDefinition> topics = loader.loadAll();
        assertThat(topics).hasSizeGreaterThanOrEqualTo(20);

        for (TopicSeedDefinition topic : topics) {
            if (topic.titles() == null) {
                continue;
            }
            topic.titles().forEach((language, byGrade) -> {
                assertThat(byGrade)
                        .as("titres vides en %s — %s", language, topic.topicId())
                        .isNotEmpty();
                byGrade.forEach((grade, label) -> {
                    assertThat(TopicTitles.GRADES)
                            .as("grade inconnu '%s' — %s", grade, topic.topicId())
                            .contains(grade);
                    assertThat(label)
                            .as("libellé vide (%s) — %s", grade, topic.topicId())
                            .isNotBlank();
                    assertThat(label.length())
                            .as("libellé trop long (%s) — %s", grade, topic.topicId())
                            .isLessThanOrEqualTo(TopicTitles.MAX_LABEL_LENGTH);
                });
            });
        }
    }
}
