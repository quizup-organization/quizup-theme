package io.github.quizup.theme.infrastructure.config;

import io.github.quizup.microservice.core.domain.constant.QuizUpConstants;
import io.github.quizup.theme.domain.exception.QuestionProblems;
import io.github.quizup.theme.domain.exception.TopicProblems;
import io.github.quizup.theme.domain.model.Question;
import io.github.quizup.theme.domain.model.QuestionStatus;
import io.github.quizup.theme.domain.model.Topic;
import io.github.quizup.theme.domain.model.TopicStatus;
import io.github.quizup.theme.domain.port.in.ApproveQuestionUseCase;
import io.github.quizup.theme.domain.port.in.CheckTopicUseCase;
import io.github.quizup.theme.domain.port.in.CreateQuestionUseCase;
import io.github.quizup.theme.domain.port.in.CreateTopicUseCase;
import io.github.quizup.theme.domain.port.in.GetQuestionUseCase;
import io.github.quizup.theme.domain.port.in.GetTopicUseCase;
import io.github.quizup.theme.domain.port.in.PublishTopicUseCase;
import io.github.quizup.theme.infrastructure.config.seed.QuestionSeedDefinition;
import io.github.quizup.theme.infrastructure.config.seed.SeedDataLoader;
import io.github.quizup.theme.infrastructure.config.seed.TopicSeedDefinition;
import io.github.quizup.theme.infrastructure.properties.AppProperties;
import org.axonframework.modelling.command.AggregateStreamCreationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletionException;

/**
 * DataSeeder - Initialise les topics et questions depuis les fichiers YAML
 * ({@code app.seed-data.location}) au demarrage.
 * <p>
 * Injecte uniquement des use cases (ports entrants) afin de respecter
 * l'architecture hexagonale du module.
 * <p>
 * Comportement :
 * - un fichier YAML invalide est ignore par {@link SeedDataLoader} sans bloquer les autres ;
 * - un theme absent est cree puis publie ;
 * - un theme deja publie est ignore ;
 * - un theme en DRAFT est repare (questions manquantes creees, questions non approuvees
 *   approuvees puis publication) ;
 * - un echec sur un theme est isole et n'empeche pas le seeding des suivants.
 * <p>
 * Le seeding demarre apres {@link ApplicationReadyEvent}, dans un thread dedie : le readiness
 * de l'application n'attend pas la fin du seed (indispensable quand le catalogue est volumineux,
 * sinon les probes Kubernetes tuent le pod en plein seed).
 * <p>
 * Active uniquement si app.seed-data.enabled=true.
 */
@Component
public class DataSeeder {

    private static final Logger logger = LoggerFactory.getLogger(DataSeeder.class);

    private static final int APPROVAL_WAIT_TIMEOUT_MS = 30_000;
    private static final int APPROVAL_POLL_INTERVAL_MS = 150;
    private static final int TOPIC_READ_TIMEOUT_MS = 10_000;

    private final CheckTopicUseCase checkTopicUseCase;
    private final CreateTopicUseCase createTopicUseCase;
    private final CreateQuestionUseCase createQuestionUseCase;
    private final ApproveQuestionUseCase approveQuestionUseCase;
    private final PublishTopicUseCase publishTopicUseCase;
    private final GetTopicUseCase getTopicUseCase;
    private final GetQuestionUseCase getQuestionUseCase;
    private final SeedDataLoader seedDataLoader;
    private final boolean seedDataEnabled;

    public DataSeeder(CheckTopicUseCase checkTopicUseCase,
                      CreateTopicUseCase createTopicUseCase,
                      CreateQuestionUseCase createQuestionUseCase,
                      ApproveQuestionUseCase approveQuestionUseCase,
                      PublishTopicUseCase publishTopicUseCase,
                      GetTopicUseCase getTopicUseCase,
                      GetQuestionUseCase getQuestionUseCase,
                      SeedDataLoader seedDataLoader,
                      AppProperties properties) {
        this.checkTopicUseCase = checkTopicUseCase;
        this.createTopicUseCase = createTopicUseCase;
        this.createQuestionUseCase = createQuestionUseCase;
        this.approveQuestionUseCase = approveQuestionUseCase;
        this.publishTopicUseCase = publishTopicUseCase;
        this.getTopicUseCase = getTopicUseCase;
        this.getQuestionUseCase = getQuestionUseCase;
        this.seedDataLoader = seedDataLoader;
        this.seedDataEnabled = properties.seedData().enabled();
    }

    /**
     * Lance le seeding en arriere-plan une fois l'application prete : le readiness n'est pas
     * bloque par la duree du seed.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        if (!seedDataEnabled) {
            return;
        }
        Thread.ofVirtual().name("theme-data-seeder").start(this::run);
    }

    public void run() {
        if (!seedDataEnabled) {
            logger.info("Data seeding is disabled (app.seed-data.enabled=false)");
            return;
        }

        logger.info("=== Starting Theme Data Seeding ===");
        long startedAt = System.currentTimeMillis();
        List<TopicSeedDefinition> definitions = seedDataLoader.loadAll();

        int created = 0;
        int repaired = 0;
        int skipped = 0;
        int failed = 0;

        for (TopicSeedDefinition definition : definitions) {
            try {
                TopicSeedOutcome outcome = seedTopic(definition);
                switch (outcome) {
                    case CREATED -> created++;
                    case REPAIRED -> repaired++;
                    case SKIPPED -> skipped++;
                }
            } catch (Exception e) {
                failed++;
                logger.error("Failed to seed topic {} ({})", definition.topicId(), definition.name(), e);
            }
        }

        logger.info("=== Theme Data Seeding Completed: {} created, {} repaired, {} skipped, {} failed in {} ms ===",
                created, repaired, skipped, failed, System.currentTimeMillis() - startedAt);
    }

    private TopicSeedOutcome seedTopic(TopicSeedDefinition definition) {
        String topicId = definition.topicId();
        boolean createdTopic = false;

        if (!checkTopicUseCase.existsByIdAndWait(topicId)) {
            try {
                createTopicUseCase.createAndWait(
                        topicId,
                        definition.name(),
                        definition.description(),
                        definition.category(),
                        null,
                        null,
                        definition.imageUrl(),
                        QuizUpConstants.SYSTEM_USER_ID
                );
                createdTopic = true;
                logger.info("Created topic '{}' ({})", definition.name(), topicId);
            } catch (CompletionException e) {
                if (!isAggregateAlreadyExists(e)) {
                    throw e;
                }
                logger.info("Topic {} already exists in the event store (projection lag), repairing", topicId);
            }
        }

        Topic topic = awaitTopic(topicId);
        if (topic.status() == TopicStatus.PUBLISHED) {
            logger.info("Topic {} is already published, skipping", topicId);
            return TopicSeedOutcome.SKIPPED;
        }
        if (topic.status() == TopicStatus.ARCHIVED) {
            logger.warn("Topic {} is archived, skipping", topicId);
            return TopicSeedOutcome.SKIPPED;
        }

        Map<String, Question> existingByText = findQuestionsByText(topicId);
        int createdQuestions = 0;
        int approvedQuestions = 0;

        for (QuestionSeedDefinition question : definition.questions()) {
            Question existing = existingByText.get(question.text());
            if (existing == null) {
                if (createQuestion(topicId, question)) {
                    createdQuestions++;
                }
            } else if (existing.status() != QuestionStatus.APPROVED) {
                approveQuestion(existing.questionId());
                approvedQuestions++;
            }
        }

        awaitApprovedQuestions(topicId, definition.questions().size());

        Topic refreshed = awaitTopic(topicId);
        if (refreshed.status() == TopicStatus.DRAFT) {
            try {
                publishTopicUseCase.publishAndWait(topicId, QuizUpConstants.SYSTEM_USER_ID);
            } catch (CompletionException e) {
                if (!isTopicNotInDraft(e)) {
                    throw e;
                }
            }
        }

        logger.info("Seeded topic '{}' ({}): {} question(s) created, {} question(s) approved",
                definition.name(), topicId, createdQuestions, approvedQuestions);

        if (createdTopic) {
            return TopicSeedOutcome.CREATED;
        }
        return createdQuestions > 0 || approvedQuestions > 0
                ? TopicSeedOutcome.REPAIRED
                : TopicSeedOutcome.SKIPPED;
    }

    private boolean createQuestion(String topicId, QuestionSeedDefinition question) {
        String questionId = UUID.randomUUID().toString();
        try {
            createQuestionUseCase.createAndWait(
                    questionId,
                    topicId,
                    question.text(),
                    question.answers(),
                    question.correctAnswer(),
                    question.imageUrl(),
                    QuizUpConstants.SYSTEM_USER_ID
            );
        } catch (CompletionException e) {
            if (!isAggregateAlreadyExists(e)) {
                throw e;
            }
            return false;
        }

        approveQuestion(questionId);
        return true;
    }

    private void approveQuestion(String questionId) {
        try {
            approveQuestionUseCase.approveAndWait(questionId, QuizUpConstants.SYSTEM_USER_ID);
        } catch (CompletionException e) {
            if (!isAlreadyApproved(e)) {
                throw e;
            }
        }
    }

    /**
     * Charge les questions du theme (read-model) indexees par texte : cle stable de reparation,
     * les identifiants de questions etant aleatoires.
     */
    private Map<String, Question> findQuestionsByText(String topicId) {
        Map<String, Question> byText = new HashMap<>();
        getQuestionUseCase.getByTopicId(topicId).join()
                .forEach(question -> byText.put(question.text(), question));
        return byText;
    }

    private Topic awaitTopic(String topicId) {
        long deadlineMs = System.currentTimeMillis() + TOPIC_READ_TIMEOUT_MS;

        while (System.currentTimeMillis() < deadlineMs) {
            try {
                return getTopicUseCase.getById(topicId).join();
            } catch (CompletionException e) {
                if (!isTopicNotFound(e)) {
                    throw e;
                }
                sleep(APPROVAL_POLL_INTERVAL_MS);
            }
        }

        throw new IllegalStateException("Timeout while waiting for topic " + topicId + " projection");
    }

    /**
     * Attend que le read-model contienne {@code expectedApprovedCount} questions approuvees.
     * Le compteur agrege {@code topic_entry.questions_counter} est volontairement ignore : il peut
     * etre transitoirement desynchronise pendant un rollout, alors que le statut des questions
     * (source de verite de la publication) est fiable.
     */
    private void awaitApprovedQuestions(String topicId, int expectedApprovedCount) {
        long deadlineMs = System.currentTimeMillis() + APPROVAL_WAIT_TIMEOUT_MS;

        while (System.currentTimeMillis() < deadlineMs) {
            long approvedCount = getQuestionUseCase.getByTopicId(topicId).join().stream()
                    .filter(question -> question.status() == QuestionStatus.APPROVED)
                    .count();

            if (approvedCount >= expectedApprovedCount) {
                return;
            }

            sleep(APPROVAL_POLL_INTERVAL_MS);
        }

        throw new IllegalStateException(
                "Timeout while waiting " + expectedApprovedCount + " approved questions for topic " + topicId);
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for projections sync", e);
        }
    }

    private boolean isAggregateAlreadyExists(Throwable throwable) {
        return hasCause(throwable, AggregateStreamCreationException.class);
    }

    private boolean isAlreadyApproved(Throwable throwable) {
        return hasCause(throwable, QuestionProblems.QuestionAlreadyApprovedProblem.class);
    }

    private boolean isTopicNotInDraft(Throwable throwable) {
        return hasCause(throwable, TopicProblems.TopicNotInDraftProblem.class);
    }

    private boolean isTopicNotFound(Throwable throwable) {
        return hasCause(throwable, TopicProblems.TopicNotFoundProblem.class);
    }

    private boolean hasCause(Throwable throwable, Class<? extends Throwable> type) {
        Throwable cause = throwable;
        while (cause != null) {
            if (type.isInstance(cause)) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }

    private enum TopicSeedOutcome {
        CREATED,
        REPAIRED,
        SKIPPED
    }
}
