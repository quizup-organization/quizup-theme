package io.github.quizup.theme.domain.port.out;

import io.github.quizup.theme.domain.model.QuestionReport;
import io.github.quizup.theme.domain.model.QuestionReportCount;
import io.github.quizup.theme.domain.model.QuestionReportSort;
import io.github.quizup.theme.domain.model.QuestionReportStatus;
import io.github.quizup.theme.domain.model.QuestionReportSummaryFilter;
import io.github.quizup.theme.domain.model.QuestionReportSummaryPage;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface QuestionReportRepositoryPort {

    void save(QuestionReport report);

    Optional<QuestionReport> findById(String reportId);

    List<QuestionReport> findByQuestionId(String questionId);

    List<QuestionReport> findOpenByQuestionId(String questionId);

    List<QuestionReportCount> findCountsByQuestionIds(List<String> questionIds);

    QuestionReportSummaryPage findSummaryPage(QuestionReportSummaryFilter filter,
                                              QuestionReportSort sort,
                                              int page,
                                              int size);

    int countDistinctOpenReportersSince(String questionId, Instant since);

    long countByStatus(QuestionReportStatus status);

    long countDistinctQuestionIdByStatus(QuestionReportStatus status);
}
