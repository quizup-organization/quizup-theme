package io.github.quizup.theme.application.projection;

import io.github.quizup.microservice.core.domain.model.i18n.Language;
import io.github.quizup.theme.domain.event.QuestionEvent;
import io.github.quizup.theme.domain.model.Question;
import io.github.quizup.theme.domain.model.QuestionContent;
import io.github.quizup.theme.domain.model.QuestionStatus;
import io.github.quizup.theme.domain.port.out.QuestionRepositoryPort;
import org.axonframework.config.ProcessingGroup;
import org.axonframework.eventhandling.EventHandler;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.UnaryOperator;

@Component
@ProcessingGroup("theme-projection")
public class QuestionProjection {

    private final QuestionRepositoryPort questionRepositoryPort;

    public QuestionProjection(QuestionRepositoryPort questionRepositoryPort) {
        this.questionRepositoryPort = questionRepositoryPort;
    }


    @EventHandler
    public void on(QuestionEvent.QuestionCreatedEvent event) {
        Question question = Question.builder()
                .questionId(event.questionId())
                .topicId(event.topicId())
                .contents(new EnumMap<>(event.contents()))
                .imageUrl(event.imageUrl())
                .correctAnswer(event.correctAnswer())
                .status(QuestionStatus.PENDING)
                .difficulty(null)
                .creatorId(event.creatorId())
                .updatedBy(event.creatorId())
                .createdAt(event.createdAt())
                .updatedAt(event.createdAt())
                .build();
        questionRepositoryPort.save(question);
    }

    @EventHandler
    public void on(QuestionEvent.QuestionTranslationsAddedEvent event) {
        questionRepositoryPort.findById(event.questionId())
                .ifPresent(question -> {
                    Map<Language, QuestionContent> contents = new EnumMap<>(question.contents());
                    contents.putAll(event.contents());

                    questionRepositoryPort.save(question.toBuilder()
                            .contents(contents)
                            .updatedBy(event.updatedBy())
                            .updatedAt(event.updatedAt())
                            .build());
                });
    }

    @EventHandler
    public void on(QuestionEvent.QuestionTranslationAddedEvent event) {
        questionRepositoryPort.findById(event.questionId())
                .ifPresent(question -> {
                    Map<Language, QuestionContent> contents = new EnumMap<>(question.contents());
                    contents.put(event.language(),
                            new QuestionContent(event.language(), event.text(), event.answers()));

                    questionRepositoryPort.save(question.toBuilder()
                            .contents(contents)
                            .updatedBy(event.updatedBy())
                            .updatedAt(event.updatedAt())
                            .build());
                });
    }

    @EventHandler
    public void on(QuestionEvent.QuestionTextUpdatedEvent event) {
        updateContent(event.questionId(), event.language(), event.updatedBy(), event.updatedAt(),
                content -> new QuestionContent(content.language(), event.text(), content.answers()));
    }

    @EventHandler
    public void on(QuestionEvent.QuestionAnswersUpdatedEvent event) {
        updateContent(event.questionId(), event.language(), event.updatedBy(), event.updatedAt(),
                content -> new QuestionContent(content.language(), content.text(), event.answers()));
    }

    @EventHandler
    public void on(QuestionEvent.QuestionCorrectAnswerUpdatedEvent event) {
        questionRepositoryPort.findById(event.questionId())
                .ifPresent(question -> questionRepositoryPort.save(question.toBuilder()
                        .correctAnswer(event.correctAnswer())
                        .updatedBy(event.updatedBy())
                        .updatedAt(event.updatedAt())
                        .build()));
    }

    @EventHandler
    public void on(QuestionEvent.QuestionImageUrlUpdatedEvent event) {
        questionRepositoryPort.findById(event.questionId())
                .ifPresent(question -> questionRepositoryPort.save(question.toBuilder()
                        .imageUrl(event.imageUrl())
                        .updatedBy(event.updatedBy())
                        .updatedAt(event.updatedAt())
                        .build()));
    }

    @EventHandler
    public void on(QuestionEvent.QuestionApprovedEvent event) {
        questionRepositoryPort.findById(event.questionId())
                .ifPresent(question -> questionRepositoryPort.save(
                        question.toBuilder()
                                .status(QuestionStatus.APPROVED)
                                .updatedBy(event.updatedBy())
                                .updatedAt(event.approvedAt())
                                .build()
                ));

    }

    @EventHandler
    public void on(QuestionEvent.QuestionRejectedEvent event) {
        questionRepositoryPort.findById(event.questionId())
                .ifPresent(question -> questionRepositoryPort.save(
                        question.toBuilder()
                                .status(QuestionStatus.REJECTED)
                                .updatedBy(event.updatedBy())
                                .updatedAt(event.rejectedAt())
                                .build()
                ));
    }

    @EventHandler
    public void on(QuestionEvent.QuestionDifficultyUpdatedEvent event) {
        questionRepositoryPort.findById(event.questionId())
                .ifPresent(question -> questionRepositoryPort.save(
                        question.toBuilder()
                                .difficulty(event.difficulty())
                                .updatedAt(event.updatedAt())
                                .build()
                ));
    }

    private void updateContent(String questionId,
                               Language language,
                               String updatedBy,
                               Instant updatedAt,
                               UnaryOperator<QuestionContent> change) {
        questionRepositoryPort.findById(questionId).ifPresent(question -> {
            QuestionContent content = question.contents().get(language);
            if (content == null) {
                return;
            }
            Map<Language, QuestionContent> contents = new EnumMap<>(question.contents());
            contents.put(language, change.apply(content));

            questionRepositoryPort.save(question.toBuilder()
                    .contents(contents)
                    .updatedBy(updatedBy)
                    .updatedAt(updatedAt)
                    .build());
        });
    }
}
