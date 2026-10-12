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
            @DefaultValue("false") boolean enabled,
            @DefaultValue("classpath*:seed/topics/*/*.yml") String location,
            /**
             * Base publique des images du seed : les {@code imageUrl} relatifs des YAML
             * (ex. {@code wikipedia/<categorie>/<fichier>}) sont résolus en {@code base + chemin}.
             * Les URLs absolues http(s) restent acceptées telles quelles.
             */
            @DefaultValue("") String imageBaseUrl) {
    }
}
