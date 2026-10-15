package io.github.quizup.theme.infrastructure.out.persistence.adapter;

import io.github.quizup.microservice.core.domain.model.i18n.Language;
import io.github.quizup.theme.domain.model.Question;
import io.github.quizup.theme.domain.model.QuestionChoice;
import io.github.quizup.theme.domain.model.QuestionDifficulty;
import io.github.quizup.theme.domain.model.QuestionStatus;
import io.github.quizup.theme.infrastructure.out.persistence.entity.QuestionContentEntity;
import io.github.quizup.theme.infrastructure.out.persistence.entity.QuestionEntity;
import io.github.quizup.theme.infrastructure.out.persistence.repository.QuestionContentJpaRepository;
import io.github.quizup.theme.infrastructure.out.persistence.repository.QuestionJpaRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tirage des questions : le read model peut porter deux lignes de **même contenu** (même texte,
 * même illustration) pour un thème — re-seed dont l'URL d'image résolue a changé, ancienne
 * question restée approuvée. Le tirage ne doit jamais les remonter toutes les deux dans une même
 * partie.
 */
class QuestionRepositoryAdapterTest {

    private final QuestionJpaRepository questionJpaRepository = mock(QuestionJpaRepository.class);
    private final QuestionContentJpaRepository contentJpaRepository =
            mock(QuestionContentJpaRepository.class);
    private final QuestionRepositoryAdapter adapter =
            new QuestionRepositoryAdapter(questionJpaRepository, contentJpaRepository);

    @Test
    void draw_deduplicates_questions_sharing_text_and_illustration() {
        givenDrawReturns(
                entity("q1", "wikipedia/flags/fr.png"),
                entity("q2", "wikipedia/flags/fr.png"),
                entity("q3", "wikipedia/flags/de.png"));
        givenContents(
                content("q1", "Quel est ce pays ?"),
                content("q2", "Quel est ce pays ?"),
                content("q3", "Quel est ce pays ?"));

        List<Question> drawn = adapter.findRandomApprovedByTopicId(
                "topic-1", 7, Set.of(Language.FR), Set.of(), Set.of());

        assertThat(drawn).extracting(Question::questionId).containsExactly("q1", "q3");
    }

    @Test
    void draw_keeps_distinct_questions_with_the_same_text_but_another_illustration() {
        givenDrawReturns(
                entity("q1", "wikipedia/flags/fr.png"),
                entity("q2", "wikipedia/flags/fr-ancien.png"));
        givenContents(content("q1", "Quel est ce pays ?"), content("q2", "Quel est ce pays ?"));

        List<Question> drawn = adapter.findRandomApprovedByTopicId(
                "topic-1", 7, Set.of(Language.FR), Set.of(), Set.of());

        assertThat(drawn).extracting(Question::questionId).containsExactly("q1", "q2");
    }

    @Test
    void draw_oversamples_so_deduplication_can_still_fill_the_round() {
        givenDrawReturns();
        givenContents();

        adapter.findRandomApprovedByTopicId("topic-1", 7, Set.of(Language.FR), Set.of(), Set.of());

        // Sur-échantillonnage borné (7 + 7) : le dédoublonnage doit pouvoir rendre 7 questions.
        verify(questionJpaRepository)
                .findRandomApprovedByTopicId(eq("topic-1"), eq(14), anyList(), eq(1L), eq(""), eq(""));
    }

    private void givenDrawReturns(QuestionEntity... entities) {
        when(questionJpaRepository.findRandomApprovedByTopicId(
                anyString(), anyInt(), anyList(), anyLong(), anyString(), anyString()))
                .thenReturn(List.of(entities));
    }

    private void givenContents(QuestionContentEntity... contents) {
        when(contentJpaRepository.findByQuestionIdIn(anyList())).thenReturn(List.of(contents));
    }

    private static QuestionEntity entity(String questionId, String imageUrl) {
        QuestionEntity entity = new QuestionEntity();
        entity.setQuestionId(questionId);
        entity.setTopicId("topic-1");
        entity.setImageUrl(imageUrl);
        entity.setCorrectAnswer(QuestionChoice.A);
        entity.setStatus(QuestionStatus.APPROVED);
        entity.setDifficulty(QuestionDifficulty.HARD);
        entity.setCreatorId("creator-1");
        return entity;
    }

    private static QuestionContentEntity content(String questionId, String text) {
        QuestionContentEntity entity = new QuestionContentEntity();
        entity.setQuestionId(questionId);
        entity.setLanguage(Language.FR.code());
        entity.setText(text);
        entity.setAnswers(Map.of(
                QuestionChoice.A, "France",
                QuestionChoice.B, "Allemagne",
                QuestionChoice.C, "Espagne",
                QuestionChoice.D, "Italie"));
        return entity;
    }
}
