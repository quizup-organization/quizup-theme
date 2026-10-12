package io.github.quizup.theme.application.handler.query;

import io.github.quizup.microservice.core.domain.model.i18n.Language;
import io.github.quizup.theme.domain.model.QuestionDifficulty;
import io.github.quizup.theme.domain.model.QuestionLanguageCoverage;
import io.github.quizup.theme.domain.port.out.QuestionRepositoryPort;
import io.github.quizup.theme.domain.query.QuestionQuery;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class QuestionQueryHandlerTest {

    private final QuestionRepositoryPort repository = mock(QuestionRepositoryPort.class);
    private final QuestionQueryHandler handler = new QuestionQueryHandler(repository);

    @Test
    void random_questions_forwards_difficulty_and_exclusions_preferences() {
        Set<Language> languages = Set.of(Language.FR);
        Set<QuestionDifficulty> difficulties = Set.of(QuestionDifficulty.HARD);
        Set<String> excluded = Set.of("question-1", "question-2");
        when(repository.findRandomApprovedByTopicId("topic-1", 7, languages, difficulties, excluded))
                .thenReturn(List.of());

        List<?> questions = handler.handle(new QuestionQuery.GetRandomApprovedQuestionsQuery(
                "topic-1", 7, languages, difficulties, excluded));

        assertThat(questions).isEmpty();
        verify(repository).findRandomApprovedByTopicId("topic-1", 7, languages, difficulties, excluded);
    }

    @Test
    void language_coverage_counts_every_non_empty_language_combination() {
        when(repository.countApprovedByTopicAndLanguages("topic-1", Set.of(Language.FR))).thenReturn(10);
        when(repository.countApprovedByTopicAndLanguages("topic-1", Set.of(Language.EN))).thenReturn(8);
        when(repository.countApprovedByTopicAndLanguages("topic-1", Set.of(Language.FR, Language.EN))).thenReturn(7);

        List<QuestionLanguageCoverage> coverage = handler.handle(
                new QuestionQuery.GetTopicLanguageCoverageQuery("topic-1"));

        assertThat(coverage).containsExactlyInAnyOrder(
                new QuestionLanguageCoverage(Set.of(Language.FR), 10),
                new QuestionLanguageCoverage(Set.of(Language.EN), 8),
                new QuestionLanguageCoverage(Set.of(Language.FR, Language.EN), 7));
    }
}
