package io.github.quizup.theme.infrastructure.out.persistence.repository;

import io.github.quizup.theme.domain.model.TopicStatus;
import io.github.quizup.theme.infrastructure.out.persistence.entity.TopicEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

/**
 * Repository JPA pour les projections de thèmes
 */
@Repository
public interface TopicJpaRepository extends JpaRepository<TopicEntity, String>, JpaSpecificationExecutor<TopicEntity> {

    @Query("""
            select t.category, count(t)
            from TopicEntity t
            where t.status = :status
              and (:normalizedName is null or t.nameNormalized like concat('%', cast(:normalizedName as string), '%'))
            group by t.category
            """)
    List<Object[]> countByCategory(@Param("status") TopicStatus status,
                                   @Param("normalizedName") String normalizedName);

    @Query("""
            select t.category, count(t)
            from TopicEntity t
            where t.status = :status
              and (:normalizedName is null or t.nameNormalized like concat('%', cast(:normalizedName as string), '%'))
              and t.topicId in :topicIds
            group by t.category
            """)
    List<Object[]> countByCategoryInIds(@Param("status") TopicStatus status,
                                        @Param("normalizedName") String normalizedName,
                                        @Param("topicIds") Collection<String> topicIds);
}
