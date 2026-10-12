package io.github.quizup.theme.application.handler.query;

import io.github.quizup.microservice.core.domain.model.i18n.Language;
import io.github.quizup.microservice.core.infrastructure.in.api.response.SearchResponse;
import io.github.quizup.theme.domain.exception.QuestionProblems;
import io.github.quizup.theme.domain.model.Question;
import io.github.quizup.theme.domain.model.QuestionLanguageCoverage;
import io.github.quizup.theme.domain.port.out.QuestionRepositoryPort;
import io.github.quizup.theme.domain.query.QuestionQuery;
import org.axonframework.queryhandling.QueryHandler;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@Component
public class QuestionQueryHandler {

    private final QuestionRepositoryPort questionRepositoryPort;

    public QuestionQueryHandler(QuestionRepositoryPort questionRepositoryPort) {
        this.questionRepositoryPort = questionRepositoryPort;
    }

    @QueryHandler
    public Question handle(QuestionQuery.GetQuestionByIdQuery query) {
        return questionRepositoryPort.findById(query.questionId())
                .orElseThrow(() -> new QuestionProblems.QuestionNotFoundProblem(query.questionId()));
    }

    @QueryHandler
    public List<Question> handle(QuestionQuery.GetQuestionsByTopicIdQuery query) {
        return questionRepositoryPort.findByTopicId(query.topicId());
    }

    @QueryHandler
    public List<Question> handle(QuestionQuery.GetRandomApprovedQuestionsQuery query) {
        return questionRepositoryPort.findRandomApprovedByTopicId(
                query.topicId(), query.count(), query.languages(),
                query.difficulties(), query.excludeQuestionIds());
    }

    @QueryHandler
    public Integer handle(QuestionQuery.CountApprovedQuestionsByTopicAndLanguagesQuery query) {
        return questionRepositoryPort.countApprovedByTopicAndLanguages(query.topicId(), query.languages());
    }

    /**
     * Couverture de toutes les combinaisons non vides des langues supportées : la façade prépare
     * ainsi une photo locale au ticket d'appariement (les événements de la saga restent
     * auto-suffisants, aucune query dans un handler de saga).
     */
    @QueryHandler
    public List<QuestionLanguageCoverage> handle(QuestionQuery.GetTopicLanguageCoverageQuery query) {
        return nonEmptyLanguageCombinations().stream()
                .map(languages -> new QuestionLanguageCoverage(
                        languages,
                        questionRepositoryPort.countApprovedByTopicAndLanguages(query.topicId(), languages)))
                .toList();
    }

    private static List<Set<Language>> nonEmptyLanguageCombinations() {
        Language[] languages = Language.values();
        List<Set<Language>> combinations = new ArrayList<>();
        for (int mask = 1; mask < (1 << languages.length); mask++) {
            Set<Language> combination = EnumSet.noneOf(Language.class);
            for (int index = 0; index < languages.length; index++) {
                if ((mask & (1 << index)) != 0) {
                    combination.add(languages[index]);
                }
            }
            combinations.add(Set.copyOf(combination));
        }
        return combinations;
    }

    @QueryHandler
    public SearchResponse<Question> handle(QuestionQuery.QuestionSearchQuery query) {
        return questionRepositoryPort.findAll(query.request());
    }
}
