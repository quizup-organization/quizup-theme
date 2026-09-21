package io.github.quizup.theme;

import io.github.quizup.theme.infrastructure.properties.AppProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(AppProperties.class)
public class ThemeServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ThemeServiceApplication.class, args);
    }
}
