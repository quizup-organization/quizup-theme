package io.github.quizup.topic.infrastructure.out.persistence.repository;

import io.github.quizup.topic.infrastructure.out.persistence.entity.TopicEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

/**
 * Repository JPA pour les projections de thèmes
 */
@Repository
public interface TopicJpaRepository extends JpaRepository<TopicEntity, String>, JpaSpecificationExecutor<TopicEntity> {

}

