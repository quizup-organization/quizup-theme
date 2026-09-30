package io.github.quizup.theme.infrastructure.out.persistence.repository;

import io.github.quizup.theme.infrastructure.out.persistence.entity.QuestionContentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface QuestionContentJpaRepository extends JpaRepository<QuestionContentEntity, Long> {

    List<QuestionContentEntity> findByQuestionIdIn(Collection<String> questionIds);

    Optional<QuestionContentEntity> findByQuestionIdAndLanguage(String questionId, String language);
}
