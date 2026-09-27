package io.github.quizup.theme.infrastructure.config.seed;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.github.quizup.theme.domain.model.QuestionChoice;
import io.github.quizup.theme.domain.model.TopicCategory;
import io.github.quizup.theme.infrastructure.properties.AppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static io.github.quizup.theme.domain.model.TopicRules.MIN_QUESTIONS_TO_PUBLISH;

/**
 * Charge les fichiers de seed YAML des thèmes ({@code app.seed-data.location}).
 *
 * <p>Un fichier = un thème, rangé par catégorie ({@code seed/topics/<categorie>/<theme>.yml}).
 * Chaque fichier est validé indépendamment : un fichier invalide est loggé puis ignoré,
 * les autres restent chargés.</p>
 */
@Component
public class SeedDataLoader {

    private static final Logger logger = LoggerFactory.getLogger(SeedDataLoader.class);

    private static final int MAX_NAME_LENGTH = 25;
    private static final int MAX_DESCRIPTION_LENGTH = 500;
    private static final int MAX_QUESTION_TEXT_LENGTH = 255;
    private static final int MAX_IMAGE_URL_LENGTH = 1024;

    private final ResourcePatternResolver resourcePatternResolver;
    private final ObjectMapper yamlMapper;
    private final String locationPattern;

    public SeedDataLoader(AppProperties properties) {
        this(properties.seedData().location(), new PathMatchingResourcePatternResolver());
    }

    SeedDataLoader(String locationPattern, ResourcePatternResolver resourcePatternResolver) {
        this.locationPattern = locationPattern;
        this.resourcePatternResolver = resourcePatternResolver;
        this.yamlMapper = new ObjectMapper(new YAMLFactory());
    }

    /**
     * Charge et valide toutes les définitions de thèmes trouvées par le pattern configuré,
     * triées par nom de fichier.
     */
    public List<TopicSeedDefinition> loadAll() {
        List<Resource> resources = discover();
        List<TopicSeedDefinition> definitions = new ArrayList<>();
        Set<String> topicIds = new HashSet<>();

        for (Resource resource : resources) {
            String filename = Objects.requireNonNullElse(resource.getFilename(), resource.getDescription());
            try {
                TopicSeedDefinition definition = load(resource);
                if (!topicIds.add(definition.topicId())) {
                    logger.error("Duplicate topic id '{}' in seed file {}, skipping", definition.topicId(), filename);
                    continue;
                }
                definitions.add(definition);
            } catch (Exception e) {
                logger.error("Invalid seed file {}, skipping: {}", filename, e.getMessage());
            }
        }

        logger.info("Loaded {} topic seed definition(s) matched by {}", definitions.size(), locationPattern);
        return definitions;
    }

    TopicSeedDefinition load(Resource resource) throws IOException {
        try (InputStream inputStream = resource.getInputStream()) {
            RawSeedFile file = yamlMapper.readValue(inputStream, RawSeedFile.class);
            return validate(file);
        }
    }

    private List<Resource> discover() {
        try {
            Resource[] resources = resourcePatternResolver.getResources(locationPattern);
            List<Resource> sorted = new ArrayList<>(List.of(resources));
            sorted.sort(Comparator.comparing(resource -> Objects.requireNonNull(resource.getFilename())));
            return sorted;
        } catch (IOException e) {
            throw new IllegalStateException("Unable to discover seed files with pattern " + locationPattern, e);
        }
    }

    private TopicSeedDefinition validate(RawSeedFile file) {
        if (file == null || file.topic() == null) {
            throw new SeedDataValidationException("missing 'topic' section");
        }

        RawSeedTopic rawTopic = file.topic();
        String topicId = requireNonBlank(rawTopic.id(), "topic.id");
        String name = requireNonBlank(rawTopic.name(), "topic.name");
        if (name.length() > MAX_NAME_LENGTH) {
            throw new SeedDataValidationException("topic.name exceeds " + MAX_NAME_LENGTH + " characters");
        }
        String description = rawTopic.description();
        if (description != null && description.length() > MAX_DESCRIPTION_LENGTH) {
            throw new SeedDataValidationException("topic.description exceeds " + MAX_DESCRIPTION_LENGTH + " characters");
        }
        TopicCategory category = parseCategory(rawTopic.category());
        String topicImageUrl = validateImageUrl(rawTopic.imageUrl(), "topic.imageUrl");

        List<RawSeedQuestion> rawQuestions = file.questions();
        if (rawQuestions == null || rawQuestions.isEmpty()) {
            throw new SeedDataValidationException("missing 'questions' section");
        }
        if (rawQuestions.size() < MIN_QUESTIONS_TO_PUBLISH) {
            throw new SeedDataValidationException(
                    "at least " + MIN_QUESTIONS_TO_PUBLISH + " questions are required to publish a topic");
        }

        List<QuestionSeedDefinition> questions = new ArrayList<>(rawQuestions.size());
        Set<String> seenTexts = new HashSet<>();
        for (int i = 0; i < rawQuestions.size(); i++) {
            RawSeedQuestion raw = rawQuestions.get(i);
            if (raw == null) {
                throw new SeedDataValidationException("questions[" + i + "] is empty");
            }
            String context = "questions[" + i + "]";
            String text = requireNonBlank(raw.text(), context + ".text");
            if (text.length() > MAX_QUESTION_TEXT_LENGTH) {
                throw new SeedDataValidationException(context + ".text exceeds " + MAX_QUESTION_TEXT_LENGTH + " characters");
            }
            if (!seenTexts.add(text)) {
                throw new SeedDataValidationException(context + ".text is duplicated: " + text);
            }

            Map<QuestionChoice, String> answers = validateAnswers(raw.answers(), context);
            QuestionChoice correctAnswer = parseChoice(raw.correctAnswer(), context + ".correctAnswer");
            if (!answers.containsKey(correctAnswer)) {
                throw new SeedDataValidationException(context + ".correctAnswer must match one of the answers");
            }

            questions.add(new QuestionSeedDefinition(
                    text,
                    answers,
                    correctAnswer,
                    validateImageUrl(raw.imageUrl(), context + ".imageUrl")));
        }

        return new TopicSeedDefinition(topicId, name, description, category, topicImageUrl, List.copyOf(questions));
    }

    private Map<QuestionChoice, String> validateAnswers(Map<String, String> rawAnswers, String context) {
        if (rawAnswers == null || rawAnswers.size() != QuestionChoice.values().length) {
            throw new SeedDataValidationException(
                    context + ".answers must define exactly the choices " + List.of(QuestionChoice.values()));
        }

        Map<QuestionChoice, String> answers = new EnumMap<>(QuestionChoice.class);
        for (QuestionChoice choice : QuestionChoice.values()) {
            String value = rawAnswers.get(choice.name());
            if (value == null || value.isBlank()) {
                throw new SeedDataValidationException(context + ".answers." + choice.name() + " is required");
            }
            answers.put(choice, value);
        }
        return Collections.unmodifiableMap(answers);
    }

    private TopicCategory parseCategory(String rawCategory) {
        String value = requireNonBlank(rawCategory, "topic.category");
        try {
            return TopicCategory.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new SeedDataValidationException("topic.category '" + value + "' is invalid (allowed: "
                    + List.of(TopicCategory.values()) + ")", e);
        }
    }

    private QuestionChoice parseChoice(String rawChoice, String context) {
        String value = requireNonBlank(rawChoice, context);
        try {
            return QuestionChoice.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new SeedDataValidationException(context + " '" + value + "' is invalid (allowed: A, B, C, D)", e);
        }
    }

    private String validateImageUrl(String rawUrl, String context) {
        if (rawUrl == null || rawUrl.isBlank()) {
            return null;
        }
        String url = rawUrl.trim();
        if (url.length() > MAX_IMAGE_URL_LENGTH) {
            throw new SeedDataValidationException(context + " exceeds " + MAX_IMAGE_URL_LENGTH + " characters");
        }
        try {
            URI uri = URI.create(url);
            String scheme = uri.getScheme();
            if (scheme == null || !(scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))) {
                throw new SeedDataValidationException(context + " must be an http(s) URL");
            }
        } catch (IllegalArgumentException e) {
            throw new SeedDataValidationException(context + " is not a valid URL: " + url, e);
        }
        return url;
    }

    private String requireNonBlank(String value, String context) {
        if (value == null || value.isBlank()) {
            throw new SeedDataValidationException(context + " is required");
        }
        return value.trim();
    }

    record RawSeedFile(RawSeedTopic topic, List<RawSeedQuestion> questions) {
    }

    record RawSeedTopic(String id, String name, String description, String category, String imageUrl) {
    }

    record RawSeedQuestion(String text, String imageUrl, Map<String, String> answers, String correctAnswer) {
    }
}
