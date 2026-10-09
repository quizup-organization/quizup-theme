package io.github.quizup.theme.application.projection;

import io.github.quizup.microservice.core.domain.model.i18n.Language;
import io.github.quizup.social.domain.event.TopicFollowerEvent;
import io.github.quizup.theme.domain.event.QuestionEvent;
import io.github.quizup.theme.domain.event.TopicEvent;
import io.github.quizup.theme.domain.model.QuestionStatus;
import io.github.quizup.theme.domain.model.Topic;
import io.github.quizup.theme.domain.model.TopicStatus;
import io.github.quizup.theme.domain.port.out.QuestionRepositoryPort;
import io.github.quizup.theme.domain.port.out.TopicFollowerRefRepositoryPort;
import io.github.quizup.theme.domain.port.out.TopicRepositoryPort;
import org.axonframework.config.ProcessingGroup;
import org.axonframework.eventhandling.EventHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.UnaryOperator;

/**
 * Projection des thèmes.
 *
 * <p>Les compteurs ({@code followersCounter}, {@code questionsCounter}) sont <b>recalculés</b>
 * à partir de leur source (ensemble d'abonnés / questions par statut) plutôt qu'incrémentés :
 * l'opération est idempotente et rejouable sans dérive.</p>
 */
@Component
@ProcessingGroup("theme-projection")
public class TopicProjection {

    private final TopicRepositoryPort topicRepositoryPort;
    private final TopicFollowerRefRepositoryPort followerRefRepositoryPort;
    private final QuestionRepositoryPort questionRepositoryPort;

    public TopicProjection(TopicRepositoryPort topicRepositoryPort,
                           TopicFollowerRefRepositoryPort followerRefRepositoryPort,
                           QuestionRepositoryPort questionRepositoryPort) {
        this.topicRepositoryPort = topicRepositoryPort;
        this.followerRefRepositoryPort = followerRefRepositoryPort;
        this.questionRepositoryPort = questionRepositoryPort;
    }

    @EventHandler
    @Transactional
    public void on(TopicEvent.TopicCreatedEvent event) {
        Map<QuestionStatus, Integer> questionsCounters = new EnumMap<>(QuestionStatus.class);
        questionsCounters.put(QuestionStatus.PENDING, 0);
        questionsCounters.put(QuestionStatus.APPROVED, 0);
        questionsCounters.put(QuestionStatus.REJECTED, 0);

        Topic topic = new Topic(
                event.topicId(),
                new EnumMap<>(event.names()),
                event.description(),
                event.category(),
                TopicStatus.DRAFT,
                event.creatorId(),
                event.creatorId(),
                0,
                questionsCounters,
                event.emoji(),
                event.color(),
                event.imageUrl(),
                event.createdAt(),
                event.createdAt()
        );
        topicRepositoryPort.save(topic);
    }

    @EventHandler
    @Transactional
    public void on(TopicEvent.TopicPublishedEvent event) {
        topicRepositoryPort.findById(event.topicId())
                .ifPresent(topic -> topicRepositoryPort.save(
                        topic.toBuilder()
                                .questionsCounter(countQuestions(event.topicId()))
                                .status(TopicStatus.PUBLISHED)
                                .updatedBy(event.updatedBy())
                                .updatedAt(event.publishedAt())
                                .build()
                ));
    }

    @EventHandler
    @Transactional
    public void on(TopicEvent.TopicNameUpdatedEvent event) {
        topicRepositoryPort.findById(event.topicId()).ifPresent(topic -> {
            Map<Language, String> names = new EnumMap<>(Language.class);
            if (topic.names() != null) {
                names.putAll(topic.names());
            }
            names.put(event.language(), event.name());
            topicRepositoryPort.save(topic.toBuilder()
                    .names(names)
                    .updatedBy(event.updatedBy())
                    .updatedAt(event.updatedAt())
                    .build());
        });
    }

    @EventHandler
    @Transactional
    public void on(TopicEvent.TopicDescriptionUpdatedEvent event) {
        updateTopic(event.topicId(), event.updatedBy(), event.updatedAt(),
                builder -> builder.description(event.description()));
    }

    @EventHandler
    @Transactional
    public void on(TopicEvent.TopicCategoryUpdatedEvent event) {
        updateTopic(event.topicId(), event.updatedBy(), event.updatedAt(),
                builder -> builder.category(event.category()));
    }

    @EventHandler
    @Transactional
    public void on(TopicEvent.TopicEmojiUpdatedEvent event) {
        updateTopic(event.topicId(), event.updatedBy(), event.updatedAt(),
                builder -> builder.emoji(event.emoji()));
    }

    @EventHandler
    @Transactional
    public void on(TopicEvent.TopicColorUpdatedEvent event) {
        updateTopic(event.topicId(), event.updatedBy(), event.updatedAt(),
                builder -> builder.color(event.color()));
    }

    @EventHandler
    @Transactional
    public void on(TopicEvent.TopicImageUrlUpdatedEvent event) {
        updateTopic(event.topicId(), event.updatedBy(), event.updatedAt(),
                builder -> builder.imageUrl(event.imageUrl()));
    }

    @EventHandler
    @Transactional
    public void on(QuestionEvent.QuestionCreatedEvent event) {
        refreshQuestionsCounter(event.topicId(), event.createdAt());
    }

    @EventHandler
    @Transactional
    public void on(QuestionEvent.QuestionApprovedEvent event) {
        refreshQuestionsCounter(event.topicId(), event.approvedAt());
    }

    @EventHandler
    @Transactional
    public void on(QuestionEvent.QuestionRejectedEvent event) {
        refreshQuestionsCounter(event.topicId(), event.rejectedAt());
    }

    // Un suivi modifie le compteur d'abonnés mais **pas** `updatedAt` : « dernière mise à jour »
    // doit refléter un changement de contenu (publication, édition, questions), pas une activité
    // sociale — sinon la section « nouveaux thèmes » de l'accueil serait polluée par les follows.
    @EventHandler
    @Transactional
    public void on(TopicFollowerEvent.TopicFollowedEvent event) {
        topicRepositoryPort.findById(event.topicId()).ifPresent(topic -> {
            followerRefRepositoryPort.add(event.topicId(), event.userId());
            topicRepositoryPort.save(topic.toBuilder()
                    .followersCounter(followerRefRepositoryPort.countByTopicId(event.topicId()))
                    .build());
        });
    }

    @EventHandler
    @Transactional
    public void on(TopicFollowerEvent.TopicUnfollowedEvent event) {
        topicRepositoryPort.findById(event.topicId()).ifPresent(topic -> {
            followerRefRepositoryPort.remove(event.topicId(), event.userId());
            topicRepositoryPort.save(topic.toBuilder()
                    .followersCounter(followerRefRepositoryPort.countByTopicId(event.topicId()))
                    .build());
        });
    }

    private void updateTopic(String topicId,
                             String updatedBy,
                             Instant updatedAt,
                             UnaryOperator<Topic.TopicBuilder> change) {
        topicRepositoryPort.findById(topicId).ifPresent(topic -> topicRepositoryPort.save(
                change.apply(topic.toBuilder())
                        .updatedBy(updatedBy)
                        .updatedAt(updatedAt)
                        .build()));
    }

    private void refreshQuestionsCounter(String topicId, Instant updatedAt) {
        Map<QuestionStatus, Integer> counters = countQuestions(topicId);
        topicRepositoryPort.findById(topicId)
                .ifPresent(topic -> topicRepositoryPort.save(
                        topic.toBuilder()
                                .questionsCounter(counters)
                                .updatedAt(updatedAt)
                                .build()
                ));
    }

    private Map<QuestionStatus, Integer> countQuestions(String topicId) {
        Map<QuestionStatus, Integer> counters = new EnumMap<>(QuestionStatus.class);
        for (QuestionStatus status : QuestionStatus.values()) {
            counters.put(status, questionRepositoryPort.countByTopicIdAndStatus(topicId, status));
        }
        return counters;
    }
}
