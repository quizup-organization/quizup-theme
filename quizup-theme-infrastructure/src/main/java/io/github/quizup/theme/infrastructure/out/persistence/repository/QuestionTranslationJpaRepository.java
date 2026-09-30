package io.github.quizup.theme.infrastructure.out.persistence.repository;

import io.github.quizup.theme.infrastructure.out.persistence.entity.QuestionTranslationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface QuestionTranslationJpaRepository extends JpaRepository<QuestionTranslationEntity, Long> {

    List<QuestionTranslationEntity> findByQuestionIdIn(Collection<String> questionIds);

    Optional<QuestionTranslationEntity> findByQuestionIdAndLanguage(String questionId, String language);
}
