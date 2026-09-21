package io.github.quizup.theme.infrastructure.out.persistence.repository;

import io.github.quizup.theme.infrastructure.out.persistence.entity.QuestionAnswerRecordEntity;
import io.github.quizup.theme.infrastructure.out.persistence.entity.QuestionAnswerRecordId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuestionAnswerRecordJpaRepository
        extends JpaRepository<QuestionAnswerRecordEntity, QuestionAnswerRecordId> {
}
