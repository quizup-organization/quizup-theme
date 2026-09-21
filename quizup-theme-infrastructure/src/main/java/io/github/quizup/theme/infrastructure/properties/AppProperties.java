package io.github.quizup.theme.infrastructure.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Propriétés applicatives de {@code quizup-theme} ({@code app.*}) en record immuable.
 */
@ConfigurationProperties("app")
public record AppProperties(
        @DefaultValue SeedData seedData) {

    public record SeedData(
            @DefaultValue("false") boolean enabled) {
    }
}
