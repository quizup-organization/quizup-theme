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
 * thème respecte une répartition par niveau.
 * <p>
 * Deux répartitions sont acceptées pendant la migration « contenu plus dur » :
 * <ul>
 *   <li>historique : 25 % par niveau (±1) ;</li>
 *   <li>cible : EASY 10 / MEDIUM 20 / HARD 35 / EXPERT 35 (±3) — les thèmes (re)générés par
 *       `seed/generate-seed.py` visent cette répartition, plus difficile.</li>
 * </ul>
 * Une fois la réécriture du catalogue terminée, retirer la répartition historique.
 */
class SeedDifficultyBalanceTest {

    private record Distribution(Map<QuestionDifficulty, Double> shares, double tolerance) {
    }

    private static final Distribution LEGACY = new Distribution(
            new EnumMap<>(Map.of(
                    QuestionDifficulty.EASY, 0.25,
                    QuestionDifficulty.MEDIUM, 0.25,
                    QuestionDifficulty.HARD, 0.25,
                    QuestionDifficulty.EXPERT, 0.25)),
            1.0);
    private static final Distribution TARGET = new Distribution(
            new EnumMap<>(Map.of(
                    QuestionDifficulty.EASY, 0.10,
                    QuestionDifficulty.MEDIUM, 0.20,
                    QuestionDifficulty.HARD, 0.35,
                    QuestionDifficulty.EXPERT, 0.35)),
            3.0);

    @Test
    void everySeedQuestionHasInitialDifficultyBalancedAcrossLevels() {
        SeedDataLoader loader = new SeedDataLoader(
                "classpath*:seed/topics/*/*.yml", "https://media.example.test/quizup-media/",
                new PathMatchingResourcePatternResolver());

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

            assertThat(matches(counts, questions.size(), LEGACY) || matches(counts, questions.size(), TARGET))
                    .as("répartition de difficulté hors cible — %s : EASY=%d MEDIUM=%d HARD=%d EXPERT=%d "
                                    + "sur %d questions (attendu 25%%±1 ou 10/20/35/35 ±3)",
                            topic.topicId(), counts.get(QuestionDifficulty.EASY),
                            counts.get(QuestionDifficulty.MEDIUM), counts.get(QuestionDifficulty.HARD),
                            counts.get(QuestionDifficulty.EXPERT), questions.size())
                    .isTrue();
        }
    }

    private static boolean matches(
            Map<QuestionDifficulty, Integer> counts, int total, Distribution distribution) {
        for (QuestionDifficulty level : QuestionDifficulty.values()) {
            double target = total * distribution.shares().get(level);
            if (Math.abs(counts.get(level) - target) > distribution.tolerance()) {
                return false;
            }
        }
        return true;
    }
}
