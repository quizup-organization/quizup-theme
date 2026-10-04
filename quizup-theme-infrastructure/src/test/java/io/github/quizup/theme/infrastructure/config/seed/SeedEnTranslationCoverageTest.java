package io.github.quizup.theme.infrastructure.config.seed;

import io.github.quizup.microservice.core.domain.model.i18n.Language;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Garde-fou de contenu : chaque question du seed réel expose FR + EN (les duels bilingues ne
 * sélectionnent que les questions disponibles dans **toutes** les langues demandées). Seul le
 * thème d'orthographe française est exclu : l'exercice porte sur des mots/règles français.
 */
class SeedEnTranslationCoverageTest {

    private static final Set<String> FRENCH_ONLY_TOPICS = Set.of("topic-orthographe-grammaire");

    @Test
    void everySeedQuestionHasEnglishTranslation() {
        SeedDataLoader loader = new SeedDataLoader(
                "classpath*:seed/topics/*/*.yml", new PathMatchingResourcePatternResolver());

        List<TopicSeedDefinition> topics = loader.loadAll();
        assertThat(topics).hasSizeGreaterThanOrEqualTo(20);

        for (TopicSeedDefinition topic : topics) {
            if (FRENCH_ONLY_TOPICS.contains(topic.topicId())) {
                continue;
            }
            for (QuestionSeedDefinition question : topic.questions()) {
                assertThat(question.contents())
                        .as("traduction EN manquante — %s : « %s »",
                                topic.topicId(),
                                question.contents().get(Language.FR).text())
                        .containsKey(Language.EN);
            }
        }
    }
}
