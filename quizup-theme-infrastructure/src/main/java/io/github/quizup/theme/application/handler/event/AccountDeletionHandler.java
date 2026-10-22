package io.github.quizup.theme.application.handler.event;

import io.github.quizup.axon.autoconfigure.erasure.AxonEventStoreErasureService;
import io.github.quizup.theme.domain.command.TopicCommand;
import io.github.quizup.identity.domain.event.UserEvent.UserAccountDeletedEvent;
import io.github.quizup.theme.domain.event.TopicEvent.TopicUnpublishedAfterAccountDeletionEvent;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.eventhandling.gateway.EventGateway;
import org.axonframework.config.ProcessingGroup;
import org.axonframework.eventhandling.EventHandler;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Supprime les contenus éditoriaux dont le compte supprimé est propriétaire : ses sujets et
 * toutes leurs questions, ainsi que les questions qu'il a créées dans les sujets d'autres
 * joueurs. Un sujet qui passe sous 7 questions approuvées repasse en DRAFT.
 */
@Component
@ProcessingGroup("account-deletion-theme")
public class AccountDeletionHandler {

    private final JdbcTemplate jdbcTemplate;
    private final AxonEventStoreErasureService eventStoreErasureService;
    private final EventGateway eventGateway;
    private final CommandGateway commandGateway;

    public AccountDeletionHandler(JdbcTemplate jdbcTemplate,
                                  AxonEventStoreErasureService eventStoreErasureService,
                                  EventGateway eventGateway,
                                  CommandGateway commandGateway) {
        this.jdbcTemplate = jdbcTemplate;
        this.eventStoreErasureService = eventStoreErasureService;
        this.eventGateway = eventGateway;
        this.commandGateway = commandGateway;
    }

    @EventHandler
    @Transactional
    public void on(UserAccountDeletedEvent event) {
        String userId = event.userId();

        List<String> ownedTopicIds = jdbcTemplate.queryForList(
                "SELECT topic_id FROM topic_entry WHERE creator_id = ?", String.class, userId);
        List<String> ownedQuestionIds = jdbcTemplate.queryForList(
                "SELECT question_id FROM question_entry WHERE creator_id = ?", String.class, userId);
        List<String> topicQuestionIds = questionIdsForTopics(ownedTopicIds);
        Set<String> erasedReportThreads = new LinkedHashSet<>(jdbcTemplate.queryForList(
                "SELECT DISTINCT thread_id FROM question_report WHERE reporter_id = ?",
                String.class, userId));
        erasedReportThreads.addAll(reportThreadIdsFor(topicQuestionIds));
        erasedReportThreads.addAll(reportThreadIdsFor(ownedQuestionIds));

        // Abonnements du joueur aux sujets d'autres propriétaires.
        List<String> followedTopicIds = jdbcTemplate.queryForList(
                "SELECT topic_id FROM topic_follower_ref WHERE user_id = ?", String.class, userId);
        jdbcTemplate.update("DELETE FROM topic_follower_ref WHERE user_id = ?", userId);
        for (String topicId : followedTopicIds) {
            jdbcTemplate.update("""
                    UPDATE topic_entry
                    SET followers_counter = (SELECT count(*) FROM topic_follower_ref WHERE topic_id = ?)
                    WHERE topic_id = ?
                    """, topicId, topicId);
        }

        // Signalements déposés par le joueur (les signalements des questions supprimées
        // disparaissent par cascade).
        jdbcTemplate.update("DELETE FROM question_report WHERE reporter_id = ?", userId);

        // Supprime les sujets propriétaires : les questions, contenus, compteurs, follows et
        // signalements associés partent par les clés étrangères ON DELETE CASCADE.
        jdbcTemplate.update("DELETE FROM topic_entry WHERE creator_id = ?", userId);

        // Questions créées par le joueur dans les sujets d'autres propriétaires.
        List<String> affectedTopicIds = jdbcTemplate.queryForList("""
                SELECT DISTINCT topic_id FROM question_entry
                WHERE creator_id = ? AND topic_id NOT IN (
                    SELECT topic_id FROM topic_entry WHERE creator_id = ?
                )
                """, String.class, userId, userId);
        jdbcTemplate.update("""
                DELETE FROM question_entry
                WHERE creator_id = ? AND topic_id NOT IN (
                    SELECT topic_id FROM topic_entry WHERE creator_id = ?
                )
                """, userId, userId);

        for (String topicId : affectedTopicIds) {
            refreshCountersAndPublication(topicId, userId);
        }

        // Les agrégats supprimés ne doivent plus être rejouables : purge des événements locaux.
        Set<String> erasedAggregates = new LinkedHashSet<>(ownedTopicIds);
        erasedAggregates.addAll(ownedQuestionIds);
        erasedAggregates.addAll(topicQuestionIds);
        erasedAggregates.addAll(erasedReportThreads);
        eventStoreErasureService.eraseAggregates(erasedAggregates);
    }

    private List<String> reportThreadIdsFor(List<String> questionIds) {
        if (questionIds.isEmpty()) {
            return List.of();
        }
        String placeholders = String.join(",", java.util.Collections.nCopies(questionIds.size(), "?"));
        return jdbcTemplate.queryForList(
                "SELECT DISTINCT thread_id FROM question_report WHERE question_id IN ("
                        + placeholders + ")",
                String.class,
                questionIds.toArray());
    }

    private List<String> questionIdsForTopics(List<String> topicIds) {
        if (topicIds.isEmpty()) {
            return List.of();
        }
        String placeholders = String.join(",", java.util.Collections.nCopies(topicIds.size(), "?"));
        return jdbcTemplate.queryForList(
                "SELECT question_id FROM question_entry WHERE topic_id IN (" + placeholders + ")",
                String.class,
                topicIds.toArray());
    }

    private void refreshCountersAndPublication(String topicId, String deletedUserId) {
        jdbcTemplate.update("DELETE FROM topic_questions_counter WHERE topic_id = ?", topicId);
        jdbcTemplate.update("""
                INSERT INTO topic_questions_counter (topic_id, question_status, counter)
                SELECT ?, status, count(*) FROM question_entry
                WHERE topic_id = ? GROUP BY status
                """, topicId, topicId);
        Integer remaining = jdbcTemplate.queryForObject("""
                SELECT count(*) FROM question_entry
                WHERE topic_id = ? AND status = 'APPROVED' AND archived = false
                """, Integer.class, topicId);
        String currentStatus = jdbcTemplate.queryForObject(
                "SELECT status FROM topic_entry WHERE topic_id = ?", String.class, topicId);
        if ("PUBLISHED".equals(currentStatus) && (remaining == null || remaining < 7)) {
            // Passage par l'agrégat : le statut ne doit pas être divergent entre l'event store
            // et la projection (contrairement à une mise à jour SQL directe).
            commandGateway.sendAndWait(new TopicCommand.DraftTopicCommand(
                    topicId, "ACCOUNT_DELETED"));
            String ownerId = jdbcTemplate.queryForObject(
                    "SELECT creator_id FROM topic_entry WHERE topic_id = ?", String.class, topicId);
            eventGateway.publish(new TopicUnpublishedAfterAccountDeletionEvent(
                    topicId,
                    ownerId,
                    deletedUserId,
                    remaining == null ? 0 : remaining,
                    Instant.now()));
        }
    }
}
