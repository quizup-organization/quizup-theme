package io.github.quizup.theme.domain.port.out;

import io.github.quizup.microservice.core.domain.model.i18n.Language;
import io.github.quizup.microservice.core.infrastructure.in.api.request.SearchRequest;
import io.github.quizup.microservice.core.infrastructure.in.api.response.SearchResponse;
import io.github.quizup.theme.domain.model.Question;
import io.github.quizup.theme.domain.model.QuestionStatus;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface QuestionRepositoryPort {

    void save(Question question);

    Optional<Question> findById(String questionId);

    List<Question> findByTopicId(String topicId);

    int countApprovedByTopicId(String topicId);

    int countByTopicIdAndStatus(String topicId, QuestionStatus status);

    /** Nombre de questions approuvées disponibles dans toutes les langues demandées. */
    int countApprovedByTopicAndLanguages(String topicId, Set<Language> languages);

    /** Questions approuvées aléatoires, disponibles dans toutes les langues demandées. */
    List<Question> findRandomApprovedByTopicId(String topicId, int count, Set<Language> languages);

    SearchResponse<Question> findAll(SearchRequest request);
}
