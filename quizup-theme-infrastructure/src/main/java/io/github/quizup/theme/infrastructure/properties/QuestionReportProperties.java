package io.github.quizup.theme.infrastructure.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

/** Configuration de la modération des signalements de questions. */
@ConfigurationProperties("quizup.question-report")
public record QuestionReportProperties(
        @DefaultValue AutoSuspension autoSuspension
) {

    public record AutoSuspension(
            @DefaultValue("true") boolean enabled,
            @DefaultValue("3") int threshold,
            @DefaultValue("P7D") Duration window
    ) {
    }
}
