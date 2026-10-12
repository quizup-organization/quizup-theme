package io.github.quizup.theme.infrastructure.config.seed;

import io.github.quizup.theme.domain.model.QuestionChoice;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Garde-fou de contenu : chaque thème du seed réel répartit ses bonnes réponses ~25 % par lettre
 * (±1). Sans ça, un joueur attentif comprend vite qu'il suffit de répondre A (constat prod :
 * 57,7 % de A au global, jusqu'à 100 % sur certains thèmes). Régénération :
 * {@code seed/rebalance-seed.py} (déterministe, idempotent).
 */
class SeedAnswerBalanceTest {

    @Test
    void everySeedTopicBalancesCorrectAnswersAcrossLetters() {
        SeedDataLoader loader = new SeedDataLoader(
                "classpath*:seed/topics/*/*.yml", new PathMatchingResourcePatternResolver());

        List<TopicSeedDefinition> topics = loader.loadAll();
        assertThat(topics).hasSizeGreaterThanOrEqualTo(20);

        for (TopicSeedDefinition topic : topics) {
            Map<QuestionChoice, Integer> counts = countAnswers(topic);
            int min = counts.values().stream().mapToInt(Integer::intValue).min().orElse(0);
            int max = counts.values().stream().mapToInt(Integer::intValue).max().orElse(0);

            assertThat(max - min)
                    .as("distribution A/B/C/D déséquilibrée — %s : %s (relancer seed/rebalance-seed.py)",
                            topic.topicId(), counts)
                    .isLessThanOrEqualTo(1);
        }
    }

    private static Map<QuestionChoice, Integer> countAnswers(TopicSeedDefinition topic) {
        Map<QuestionChoice, Integer> counts = new EnumMap<>(QuestionChoice.class);
        for (QuestionChoice choice : QuestionChoice.values()) {
            counts.put(choice, 0);
        }
        for (QuestionSeedDefinition question : topic.questions()) {
            counts.merge(question.correctAnswer(), 1, Integer::sum);
        }
        return counts;
    }
}
