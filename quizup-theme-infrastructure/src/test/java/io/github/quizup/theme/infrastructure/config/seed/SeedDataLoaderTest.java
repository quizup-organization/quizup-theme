package io.github.quizup.theme.infrastructure.config.seed;

import io.github.quizup.theme.domain.model.TopicCategory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SeedDataLoaderTest {

    private static final String ALL_TOPICS_PATTERN = "classpath*:seed/topics/*/*.yml";

    private final SeedDataLoader loader =
            new SeedDataLoader(ALL_TOPICS_PATTERN, new PathMatchingResourcePatternResolver());

    @Test
    void loadsAndValidatesEveryBundledSeedFile() throws IOException {
        Resource[] resources = new PathMatchingResourcePatternResolver().getResources(ALL_TOPICS_PATTERN);

        assertThat(resources).hasSizeGreaterThanOrEqualTo(20);

        List<TopicSeedDefinition> definitions = loader.loadAll();

        assertThat(definitions).hasSameSizeAs(resources);
        assertThat(definitions).extracting(TopicSeedDefinition::topicId).doesNotHaveDuplicates();
        assertThat(definitions).allSatisfy(definition -> {
            assertThat(definition.name()).isNotBlank().hasSizeLessThanOrEqualTo(25);
            assertThat(definition.category()).isNotNull();
            assertThat(definition.questions()).hasSizeGreaterThanOrEqualTo(7);
            assertThat(definition.questions()).extracting(QuestionSeedDefinition::text).doesNotHaveDuplicates();
            assertThat(definition.questions()).allSatisfy(question -> {
                assertThat(question.answers()).hasSize(4);
                assertThat(question.answers()).containsKey(question.correctAnswer());
            });
            assertThat(definition.questions())
                    .extracting(QuestionSeedDefinition::imageUrl)
                    .filteredOn(Objects::nonNull)
                    .allSatisfy(url -> assertThat(url).startsWith("http"));
        });
    }

    @Test
    void loadAllSkipsInvalidFilesAndKeepsValidOnes() {
        SeedDataLoader isolated =
                new SeedDataLoader("classpath*:seed-test/*/*.yml", new PathMatchingResourcePatternResolver());

        List<TopicSeedDefinition> definitions = isolated.loadAll();

        assertThat(definitions).hasSize(1);
        assertThat(definitions.get(0).topicId()).isEqualTo("topic-test-fixture");
        assertThat(definitions.get(0).category()).isEqualTo(TopicCategory.GENERAL);
        assertThat(definitions.get(0).questions()).hasSize(7);
    }

    @Test
    void rejectsTopicWithTooFewQuestions(@TempDir Path tempDir) throws IOException {
        Resource resource = writeYaml(tempDir, yamlWithQuestions("Q1 ?", "Q2 ?"));

        assertThatThrownBy(() -> loader.load(resource))
                .isInstanceOf(SeedDataValidationException.class)
                .hasMessageContaining("at least");
    }

    @Test
    void rejectsDuplicateQuestionTexts(@TempDir Path tempDir) throws IOException {
        Resource resource = writeYaml(tempDir, yamlWithQuestions(
                "Q1 ?", "Q2 ?", "Q3 ?", "Q1 ?", "Q5 ?", "Q6 ?", "Q7 ?"));

        assertThatThrownBy(() -> loader.load(resource))
                .isInstanceOf(SeedDataValidationException.class)
                .hasMessageContaining("duplicated");
    }

    @Test
    void rejectsNonHttpImageUrl(@TempDir Path tempDir) throws IOException {
        String yaml = """
                topic:
                  id: topic-x
                  name: "X"
                  category: GENERAL
                questions:
                """;
        StringBuilder questions = new StringBuilder(yaml);
        for (int i = 1; i <= 7; i++) {
            questions.append("""
                      - text: "Q%d ?"
                        imageUrl: "ftp://example.com/image.jpg"
                        answers: { A: a, B: b, C: c, D: d }
                        correctAnswer: A
                    """.formatted(i));
        }

        assertThatThrownBy(() -> loader.load(writeYaml(tempDir, questions.toString())))
                .isInstanceOf(SeedDataValidationException.class)
                .hasMessageContaining("http(s) URL");
    }

    @Test
    void rejectsUnknownCategory(@TempDir Path tempDir) throws IOException {
        Resource resource = writeYaml(tempDir, """
                topic:
                  id: topic-x
                  name: "X"
                  category: NOT_A_CATEGORY
                questions:
                """);

        assertThatThrownBy(() -> loader.load(resource))
                .isInstanceOf(SeedDataValidationException.class)
                .hasMessageContaining("topic.category");
    }

    @Test
    void rejectsBlankTopicId(@TempDir Path tempDir) throws IOException {
        Resource resource = writeYaml(tempDir, """
                topic:
                  name: "X"
                  category: GENERAL
                questions:
                """);

        assertThatThrownBy(() -> loader.load(resource))
                .isInstanceOf(SeedDataValidationException.class)
                .hasMessageContaining("topic.id");
    }

    private static String yamlWithQuestions(String... texts) {
        StringBuilder yaml = new StringBuilder("""
                topic:
                  id: topic-x
                  name: "X"
                  category: GENERAL
                questions:
                """);
        for (String text : texts) {
            yaml.append("""
                      - text: "%s"
                        answers: { A: a, B: b, C: c, D: d }
                        correctAnswer: A
                    """.formatted(text));
        }
        return yaml.toString();
    }

    private static Resource writeYaml(Path tempDir, String content) throws IOException {
        Path file = tempDir.resolve("seed-" + System.nanoTime() + ".yml");
        Files.writeString(file, content);
        return new FileSystemResource(file);
    }
}
