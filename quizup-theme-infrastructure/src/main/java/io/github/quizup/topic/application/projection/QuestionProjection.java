package io.github.quizup.topic.application.projection;

import io.github.quizup.topic.domain.event.QuestionEvent;
import io.github.quizup.topic.domain.model.Question;
import io.github.quizup.topic.domain.model.QuestionStatus;
import io.github.quizup.topic.domain.port.out.QuestionRepositoryPort;
import org.axonframework.eventhandling.EventHandler;
import org.springframework.stereotype.Component;

@Component
public class QuestionProjection {

    private final QuestionRepositoryPort questionRepositoryPort;

    public QuestionProjection(QuestionRepositoryPort questionRepositoryPort) {
        this.questionRepositoryPort = questionRepositoryPort;
    }


    @EventHandler
    public void on(QuestionEvent.QuestionCreatedEvent event) {
        Question question = new Question(
                event.questionId(),
                event.topicId(),
                event.text(),
                event.answers(),
                event.correctAnswer(),
                QuestionStatus.PENDING,
                event.creatorId(),
                event.creatorId(),
                event.createdAt(),
                event.createdAt()
        );
        questionRepositoryPort.save(question);
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
}
