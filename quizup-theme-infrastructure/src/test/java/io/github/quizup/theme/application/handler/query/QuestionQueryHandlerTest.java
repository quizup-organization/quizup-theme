package io.github.quizup.theme.application.handler.query;

import io.github.quizup.microservice.core.domain.model.i18n.Language;
import io.github.quizup.theme.domain.model.QuestionDifficulty;
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
}
