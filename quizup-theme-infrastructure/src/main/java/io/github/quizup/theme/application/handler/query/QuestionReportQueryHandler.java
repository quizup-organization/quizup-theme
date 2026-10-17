package io.github.quizup.theme.application.handler.query;

import io.github.quizup.theme.domain.exception.QuestionReportProblems;
import io.github.quizup.theme.domain.model.QuestionReport;
import io.github.quizup.theme.domain.model.QuestionReportCount;
import io.github.quizup.theme.domain.model.QuestionReportStats;
import io.github.quizup.theme.domain.model.QuestionReportStatus;
import io.github.quizup.theme.domain.model.QuestionReportSummaryPage;
import io.github.quizup.theme.domain.port.out.QuestionReportRepositoryPort;
import io.github.quizup.theme.domain.port.out.QuestionRepositoryPort;
import io.github.quizup.theme.domain.query.QuestionReportQuery;
import org.axonframework.queryhandling.QueryHandler;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class QuestionReportQueryHandler {

    private final QuestionReportRepositoryPort reportRepository;
    private final QuestionRepositoryPort questionRepository;

    public QuestionReportQueryHandler(QuestionReportRepositoryPort reportRepository,
                                      QuestionRepositoryPort questionRepository) {
        this.reportRepository = reportRepository;
        this.questionRepository = questionRepository;
    }

    @QueryHandler
    public QuestionReport handle(QuestionReportQuery.GetQuestionReportByIdQuery query) {
        return reportRepository.findById(query.reportId())
                .orElseThrow(() -> new QuestionReportProblems.QuestionReportNotFoundProblem(query.reportId()));
    }

    @QueryHandler
    public List<QuestionReport> handle(QuestionReportQuery.GetQuestionReportsByQuestionQuery query) {
        return reportRepository.findByQuestionId(query.questionId());
    }

    @QueryHandler
    public List<QuestionReportCount> handle(QuestionReportQuery.GetQuestionReportCountsQuery query) {
        return reportRepository.findCountsByQuestionIds(query.questionIds());
    }

    @QueryHandler
    public QuestionReportSummaryPage handle(QuestionReportQuery.GetReportedQuestionPageQuery query) {
        return reportRepository.findSummaryPage(
                query.filter(), query.sort(), query.page(), query.size());
    }

    @QueryHandler
    public QuestionReportStats handle(QuestionReportQuery.GetQuestionReportStatsQuery query) {
        return new QuestionReportStats(
                reportRepository.countByStatus(QuestionReportStatus.OPEN),
                reportRepository.countDistinctQuestionIdByStatus(QuestionReportStatus.OPEN),
                questionRepository.countSuspended(),
                questionRepository.countArchived());
    }
}
