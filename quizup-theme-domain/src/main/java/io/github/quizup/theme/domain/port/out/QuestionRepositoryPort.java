package io.github.quizup.theme.domain.port.out;

import io.github.quizup.microservice.core.domain.model.i18n.Language;
import io.github.quizup.microservice.core.infrastructure.in.api.request.SearchRequest;
import io.github.quizup.microservice.core.infrastructure.in.api.response.SearchResponse;
import io.github.quizup.theme.domain.model.Question;
import io.github.quizup.theme.domain.model.QuestionAdminFilter;
import io.github.quizup.theme.domain.model.QuestionAdminPage;
import io.github.quizup.theme.domain.model.QuestionAdminSort;
import io.github.quizup.theme.domain.model.QuestionDifficulty;
import io.github.quizup.theme.domain.model.QuestionStatus;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface QuestionRepositoryPort {

    void save(Question question);

    Optional<Question> findById(String questionId);

    List<Question> findByTopicId(String topicId);

    List<Question> findByIds(List<String> questionIds);

    QuestionAdminPage findAdminPage(QuestionAdminFilter filter,
                                    QuestionAdminSort sort,
                                    int page,
                                    int size);

    int countSuspended();

    int countArchived();

    int countApprovedByTopicId(String topicId);

    int countByTopicIdAndStatus(String topicId, QuestionStatus status);

    /** Nombre de questions approuvées disponibles dans toutes les langues demandées. */
    int countApprovedByTopicAndLanguages(String topicId, Set<Language> languages);

    /**
     * Questions approuvées aléatoires, disponibles dans toutes les langues demandées.
     * {@code difficulties} et {@code excludeQuestionIds} sont des préférences de tri best-effort
     * (jamais-jouées &gt; difficulté cible &gt; difficulté inconnue &gt; autres).
     */
    List<Question> findRandomApprovedByTopicId(String topicId, int count, Set<Language> languages,
                                               Set<QuestionDifficulty> difficulties,
                                               Set<String> excludeQuestionIds);

    SearchResponse<Question> findAll(SearchRequest request);
}
