package io.github.quizup.theme.infrastructure.config.seed;

import io.github.quizup.theme.domain.model.QuestionDifficulty;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Garde-fou de contenu : chaque question du seed réel porte une difficulté initiale et chaque
 * thème respecte une répartition équilibrée sur les quatre niveaux (25 % par niveau, tolérance
 * ±1 ; répartition proportionnelle pour les thèmes de plus de 100 questions).
 */
class SeedDifficultyBalanceTest {

    private static final double TOLERANCE = 1.0;

    @Test
    void everySeedQuestionHasInitialDifficultyBalancedAcrossLevels() {
        SeedDataLoader loader = new SeedDataLoader(
                "classpath*:seed/topics/*/*.yml", new PathMatchingResourcePatternResolver());

        List<TopicSeedDefinition> topics = loader.loadAll();
        assertThat(topics).hasSizeGreaterThanOrEqualTo(20);

        for (TopicSeedDefinition topic : topics) {
            List<QuestionSeedDefinition> questions = topic.questions();

            assertThat(questions)
                    .as("difficulté initiale manquante — %s", topic.topicId())
                    .allSatisfy(question -> assertThat(question.difficulty()).isNotNull());

            Map<QuestionDifficulty, Integer> counts = new EnumMap<>(QuestionDifficulty.class);
            for (QuestionDifficulty level : QuestionDifficulty.values()) {
                counts.put(level, 0);
            }
            questions.forEach(question -> counts.merge(question.difficulty(), 1, Integer::sum));

            double target = questions.size() / (double) QuestionDifficulty.values().length;
            for (QuestionDifficulty level : QuestionDifficulty.values()) {
                assertThat((double) Math.abs(counts.get(level) - target))
                        .as("répartition de difficulté déséquilibrée — %s : %s=%d sur %d questions (cible %.2f ±%.1f)",
                                topic.topicId(), level, counts.get(level), questions.size(), target, TOLERANCE)
                        .isLessThanOrEqualTo(TOLERANCE);
            }
        }
    }
}
