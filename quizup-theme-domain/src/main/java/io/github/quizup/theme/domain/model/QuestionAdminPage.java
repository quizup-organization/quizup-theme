package io.github.quizup.theme.domain.model;

import java.util.List;

/** Page du catalogue admin. */
public record QuestionAdminPage(
        List<Question> questions,
        long totalElements
) {
    public QuestionAdminPage {
        questions = questions == null ? List.of() : List.copyOf(questions);
    }
}
