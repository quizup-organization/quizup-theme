package io.github.quizup.theme.infrastructure.config;

import io.github.quizup.common.domain.constant.QuizUpConstants;
import io.github.quizup.theme.domain.model.QuestionChoice;
import io.github.quizup.theme.domain.model.QuestionStatus;
import io.github.quizup.theme.domain.model.Topic;
import io.github.quizup.theme.domain.model.TopicCategory;
import io.github.quizup.theme.domain.port.in.GetTopicUseCase;
import io.github.quizup.theme.domain.port.in.CreateQuestionUseCase;
import io.github.quizup.theme.domain.port.in.ApproveQuestionUseCase;
import io.github.quizup.theme.domain.port.in.CheckTopicUseCase;
import io.github.quizup.theme.domain.port.in.CreateTopicUseCase;
import io.github.quizup.theme.domain.port.in.PublishTopicUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

import static io.github.quizup.theme.domain.model.TopicRules.MIN_QUESTIONS_TO_PUBLISH;

/**
 * DataSeeder - Initialise les topics et questions de test au demarrage.
 * <p>
 * Injecte uniquement des use cases (ports entrants) afin de respecter
 * l'architecture hexagonale du module.
 * <p>
 * Active uniquement si app.seed-data.enabled=true.
 */
@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DataSeeder.class);

    private final CheckTopicUseCase checkTopicUseCase;
    private final CreateTopicUseCase createTopicUseCase;
    private final CreateQuestionUseCase createQuestionUseCase;
    private final ApproveQuestionUseCase approveQuestionUseCase;
    private final PublishTopicUseCase publishTopicUseCase;
    private final GetTopicUseCase getTopicUseCase;

    @Value("${app.seed-data.enabled:false}")
    private boolean seedDataEnabled;

    public DataSeeder(CheckTopicUseCase checkTopicUseCase,
                      CreateTopicUseCase createTopicUseCase,
                      CreateQuestionUseCase createQuestionUseCase,
                      ApproveQuestionUseCase approveQuestionUseCase,
                      PublishTopicUseCase publishTopicUseCase,
                      GetTopicUseCase getTopicUseCase) {
        this.checkTopicUseCase = checkTopicUseCase;
        this.createTopicUseCase = createTopicUseCase;
        this.createQuestionUseCase = createQuestionUseCase;
        this.approveQuestionUseCase = approveQuestionUseCase;
        this.publishTopicUseCase = publishTopicUseCase;
        this.getTopicUseCase = getTopicUseCase;
    }

    @Override
    public void run(String... args) {
        if (!seedDataEnabled) {
            logger.info("Data seeding is disabled (app.seed-data.enabled=false)");
            return;
        }

        logger.info("=== Starting Theme Data Seeding ===");

        try {
            seedScienceTopic();
            seedHistoryTopic();
            seedPokemonGen1Topic();
            seedPokemonGen2Topic();
            logger.info("=== Theme Data Seeding Completed Successfully ===");
        } catch (Exception e) {
            logger.error("Error during theme data seeding", e);
        }
    }

    private void seedScienceTopic() {
        String topicId = "topic-science-001";

        boolean exists = checkTopicUseCase.existsByIdAndWait(topicId);

        if (exists) {
            logger.info("Science topic already exists, skipping creation");
            return;
        }

        logger.info("Creating Science topic...");

        createTopicUseCase.createAndWait(
                topicId,
                "Sciences Générales",
                "Testez vos connaissances scientifiques !",
                TopicCategory.SCIENCE,
                QuizUpConstants.ADMIN_USER_ID
        );

        String[] questionIds = addScienceQuestions(topicId);

        for (String qId : questionIds) {
            approveQuestionUseCase.approveAndWait(qId, QuizUpConstants.ADMIN_USER_ID);
        }

        awaitApprovedQuestionsCounter(topicId, MIN_QUESTIONS_TO_PUBLISH);
        publishTopicUseCase.publishAndWait(topicId, QuizUpConstants.ADMIN_USER_ID);

        logger.info("✓ Science topic created and published: {}", topicId);
    }

    private String[] addScienceQuestions(String topicId) {
        String[] ids = new String[20];
        Object[][] questions = {
                {"Quelle est la formule chimique de l'eau ?", Map.of(QuestionChoice.A, "H2O", QuestionChoice.B, "CO2", QuestionChoice.C, "NaCl", QuestionChoice.D, "H2O2"), QuestionChoice.A},
                {"Combien y a-t-il de planètes dans le système solaire ?", Map.of(QuestionChoice.A, "7", QuestionChoice.B, "8", QuestionChoice.C, "9", QuestionChoice.D, "10"), QuestionChoice.B},
                {"Quelle est la vitesse approximative de la lumière en km/s ?", Map.of(QuestionChoice.A, "300 000", QuestionChoice.B, "150 000", QuestionChoice.C, "450 000", QuestionChoice.D, "100 000"), QuestionChoice.A},
                {"Quel est l'élément le plus abondant dans l'univers ?", Map.of(QuestionChoice.A, "Hydrogène", QuestionChoice.B, "Hélium", QuestionChoice.C, "Oxygène", QuestionChoice.D, "Carbone"), QuestionChoice.A},
                {"Quel gaz les plantes absorbent-elles pour la photosynthèse ?", Map.of(QuestionChoice.A, "CO2", QuestionChoice.B, "O2", QuestionChoice.C, "N2", QuestionChoice.D, "H2"), QuestionChoice.A},
                {"Quelle est l'unité de mesure de la fréquence ?", Map.of(QuestionChoice.A, "Hertz", QuestionChoice.B, "Volt", QuestionChoice.C, "Ampère", QuestionChoice.D, "Newton"), QuestionChoice.A},
                {"Quel est le symbole chimique du fer ?", Map.of(QuestionChoice.A, "Fe", QuestionChoice.B, "Fr", QuestionChoice.C, "Fi", QuestionChoice.D, "Fo"), QuestionChoice.A},
                {"Quel est l'organe vital responsable de la circulation sanguine ?", Map.of(QuestionChoice.A, "Poumon", QuestionChoice.B, "Cœur", QuestionChoice.C, "Cerveau", QuestionChoice.D, "Foie"), QuestionChoice.B},
                {"Combien de chromosomes un être humain possède-t-il ?", Map.of(QuestionChoice.A, "23", QuestionChoice.B, "46", QuestionChoice.C, "23 paires", QuestionChoice.D, "48"), QuestionChoice.B},
                {"Quel processus permet aux organismes de se reproduire asexuellement ?", Map.of(QuestionChoice.A, "Mitose", QuestionChoice.B, "Méiose", QuestionChoice.C, "Photosynthèse", QuestionChoice.D, "Respiration"), QuestionChoice.A},
                {"Quel est le point de fusion de la glace en Celsius ?", Map.of(QuestionChoice.A, "0°C", QuestionChoice.B, "-10°C", QuestionChoice.C, "100°C", QuestionChoice.D, "-40°C"), QuestionChoice.A},
                {"Quel gaz les animaux respirent-ils ?", Map.of(QuestionChoice.A, "Azote", QuestionChoice.B, "Oxygène", QuestionChoice.C, "Dioxyde de carbone", QuestionChoice.D, "Hydrogène"), QuestionChoice.B},
                {"Quelle est la plus grande planète du système solaire ?", Map.of(QuestionChoice.A, "Saturne", QuestionChoice.B, "Jupiter", QuestionChoice.C, "Neptune", QuestionChoice.D, "Uranus"), QuestionChoice.B},
                {"Combien de temps la lumière du Soleil met-elle pour atteindre la Terre ?", Map.of(QuestionChoice.A, "8 secondes", QuestionChoice.B, "8 minutes", QuestionChoice.C, "8 heures", QuestionChoice.D, "1 jour"), QuestionChoice.B},
                {"Quel type de liaisons chimiques existe entre les atomes d'hydrogène et d'oxygène dans l'eau ?", Map.of(QuestionChoice.A, "Liaisons ioniques", QuestionChoice.B, "Liaisons covalentes", QuestionChoice.C, "Liaisons métalliques", QuestionChoice.D, "Liaisons faibles"), QuestionChoice.B},
                {"Quel est le pH neutre ?", Map.of(QuestionChoice.A, "0", QuestionChoice.B, "7", QuestionChoice.C, "14", QuestionChoice.D, "10"), QuestionChoice.B},
                {"Quel est le plus grand océan du monde ?", Map.of(QuestionChoice.A, "Océan Atlantique", QuestionChoice.B, "Océan Indien", QuestionChoice.C, "Océan Pacifique", QuestionChoice.D, "Océan Arctique"), QuestionChoice.C},
                {"Quelle est la température de fusion du fer en Celsius ?", Map.of(QuestionChoice.A, "100°C", QuestionChoice.B, "500°C", QuestionChoice.C, "1538°C", QuestionChoice.D, "3000°C"), QuestionChoice.C},
                {"Quel gaz crée l'effet de serre ?", Map.of(QuestionChoice.A, "Dioxyde de carbone", QuestionChoice.B, "Monoxyde de carbone", QuestionChoice.C, "Méthane", QuestionChoice.D, "Dioxyde de soufre"), QuestionChoice.A},
                {"Combien de sens avons-nous ?", Map.of(QuestionChoice.A, "4", QuestionChoice.B, "5", QuestionChoice.C, "6", QuestionChoice.D, "Plus de 5"), QuestionChoice.D},
        };

        for (int i = 0; i < questions.length; i++) {
            ids[i] = UUID.randomUUID().toString();
            createQuestionUseCase.createAndWait(
                    ids[i],
                    topicId,
                    (String) questions[i][0],
                    (Map<QuestionChoice, String>) questions[i][1],
                    (QuestionChoice) questions[i][2],
                    QuizUpConstants.ADMIN_USER_ID
            );
        }
        return ids;
    }

    private void seedHistoryTopic() {
        String topicId = "topic-history-001";

        boolean exists = checkTopicUseCase.existsByIdAndWait(topicId);

        if (exists) {
            logger.info("History topic already exists, skipping creation");
            return;
        }

        logger.info("Creating History topic...");

        createTopicUseCase.createAndWait(
                topicId,
                "Histoire Mondiale",
                "Connaissez-vous l'histoire ?",
                TopicCategory.HISTORY,
                QuizUpConstants.ADMIN_USER_ID
        );

        String[] questionIds = addHistoryQuestions(topicId);

        for (String qId : questionIds) {
            approveQuestionUseCase.approveAndWait(qId, QuizUpConstants.ADMIN_USER_ID);
        }

        awaitApprovedQuestionsCounter(topicId, MIN_QUESTIONS_TO_PUBLISH);
        publishTopicUseCase.publishAndWait(topicId, QuizUpConstants.ADMIN_USER_ID);
        logger.info("✓ History topic created and published: {}", topicId);
    }

    private String[] addHistoryQuestions(String topicId) {
        String[] ids = new String[20];
        Object[][] questions = {
                {"En quelle année a débuté la Première Guerre Mondiale ?", Map.of(QuestionChoice.A, "1914", QuestionChoice.B, "1918", QuestionChoice.C, "1939", QuestionChoice.D, "1900"), QuestionChoice.A},
                {"Qui était le premier président des États-Unis ?", Map.of(QuestionChoice.A, "George Washington", QuestionChoice.B, "Abraham Lincoln", QuestionChoice.C, "Thomas Jefferson", QuestionChoice.D, "John Adams"), QuestionChoice.A},
                {"En quelle année a eu lieu la Révolution française ?", Map.of(QuestionChoice.A, "1789", QuestionChoice.B, "1776", QuestionChoice.C, "1804", QuestionChoice.D, "1815"), QuestionChoice.A},
                {"Quel empire était le plus étendu géographiquement en son apogée ?", Map.of(QuestionChoice.A, "Empire mongol", QuestionChoice.B, "Empire romain", QuestionChoice.C, "Empire britannique", QuestionChoice.D, "Empire ottoman"), QuestionChoice.A},
                {"En quelle année Napoléon a-t-il été exilé à Sainte-Hélène ?", Map.of(QuestionChoice.A, "1815", QuestionChoice.B, "1812", QuestionChoice.C, "1820", QuestionChoice.D, "1810"), QuestionChoice.A},
                {"Qui a construit les pyramides de Gizeh ?", Map.of(QuestionChoice.A, "Les Égyptiens", QuestionChoice.B, "Les Romains", QuestionChoice.C, "Les Grecs", QuestionChoice.D, "Les Phéniciens"), QuestionChoice.A},
                {"En quelle année le mur de Berlin est-il tombé ?", Map.of(QuestionChoice.A, "1989", QuestionChoice.B, "1991", QuestionChoice.C, "1985", QuestionChoice.D, "1987"), QuestionChoice.A},
                {"Qui était le leader de l'Allemagne nazie ?", Map.of(QuestionChoice.A, "Adolf Hitler", QuestionChoice.B, "Benito Mussolini", QuestionChoice.C, "Francisco Franco", QuestionChoice.D, "Joseph Staline"), QuestionChoice.A},
                {"En quelle année la Déclaration d'Indépendance américaine a-t-elle été signée ?", Map.of(QuestionChoice.A, "1776", QuestionChoice.B, "1774", QuestionChoice.C, "1778", QuestionChoice.D, "1781"), QuestionChoice.A},
                {"Quel était le nom de la capitale de l'Empire romain ?", Map.of(QuestionChoice.A, "Rome", QuestionChoice.B, "Byzance", QuestionChoice.C, "Athènes", QuestionChoice.D, "Carthage"), QuestionChoice.A},
                {"En quelle année la Révolution russe a-t-elle eu lieu ?", Map.of(QuestionChoice.A, "1905", QuestionChoice.B, "1917", QuestionChoice.C, "1922", QuestionChoice.D, "1945"), QuestionChoice.B},
                {"Qui a découvert l'Amérique en 1492 ?", Map.of(QuestionChoice.A, "Christophe Colomb", QuestionChoice.B, "Amerigo Vespucci", QuestionChoice.C, "Bartolomeu Dias", QuestionChoice.D, "Vasco de Gama"), QuestionChoice.A},
                {"En quelle année la Seconde Guerre Mondiale a-t-elle commencé ?", Map.of(QuestionChoice.A, "1937", QuestionChoice.B, "1938", QuestionChoice.C, "1939", QuestionChoice.D, "1940"), QuestionChoice.C},
                {"Quel était l'objectif principal de la Magna Carta en 1215 ?", Map.of(QuestionChoice.A, "Limiter le pouvoir du roi", QuestionChoice.B, "Augmenter le pouvoir du pape", QuestionChoice.C, "Créer une démocratie", QuestionChoice.D, "Établir un empire"), QuestionChoice.A},
                {"Qui était le premier empereur de France ?", Map.of(QuestionChoice.A, "Louis XIV", QuestionChoice.B, "Napoléon Bonaparte", QuestionChoice.C, "Charles le Grand", QuestionChoice.D, "Henri IV"), QuestionChoice.B},
                {"En quelle année Constantinople a-t-elle été conquise par les Ottomans ?", Map.of(QuestionChoice.A, "1453", QuestionChoice.B, "1389", QuestionChoice.C, "1571", QuestionChoice.D, "1683"), QuestionChoice.A},
                {"Quel événement a marqué la fin de la Préhistoire ?", Map.of(QuestionChoice.A, "L'invention de l'écriture", QuestionChoice.B, "La domestication du feu", QuestionChoice.C, "La création de l'agriculture", QuestionChoice.D, "La construction des pyramides"), QuestionChoice.A},
                {"Qui était le leader de l'Union soviétique pendant la Seconde Guerre Mondiale ?", Map.of(QuestionChoice.A, "Lénine", QuestionChoice.B, "Staline", QuestionChoice.C, "Trotski", QuestionChoice.D, "Khrouchtchev"), QuestionChoice.B},
                {"En quelle année l'Allemagne s'est-elle unifiée sous Bismarck ?", Map.of(QuestionChoice.A, "1871", QuestionChoice.B, "1848", QuestionChoice.C, "1888", QuestionChoice.D, "1864"), QuestionChoice.A},
                {"Quel était le nom du système de séparation raciale en Afrique du Sud ?", Map.of(QuestionChoice.A, "Apartheid", QuestionChoice.B, "Colonialisme", QuestionChoice.C, "Ségrégationnisme", QuestionChoice.D, "Fascisme"), QuestionChoice.A},
        };

        for (int i = 0; i < questions.length; i++) {
            ids[i] = UUID.randomUUID().toString();
            createQuestionUseCase.createAndWait(
                    ids[i],
                    topicId,
                    (String) questions[i][0],
                    (Map<QuestionChoice, String>) questions[i][1],
                    (QuestionChoice) questions[i][2],
                    QuizUpConstants.ADMIN_USER_ID
            );
        }
        return ids;
    }

    private void seedPokemonGen1Topic() {
        String topicId = "topic-pokemon-gen1-001";

        boolean exists = checkTopicUseCase.existsByIdAndWait(topicId);

        if (exists) {
            logger.info("Pokemon Gen1 topic already exists, skipping creation");
            return;
        }

        logger.info("Creating Pokemon Gen1 topic...");

        createTopicUseCase.createAndWait(
                topicId,
                "Pokémon 1G",
                "Attrapez-les tous ! Testez vos connaissances sur la 1ère génération Pokémon !",
                TopicCategory.GAMES,
                QuizUpConstants.ADMIN_USER_ID
        );

        String[] questionIds = addPokemonGen1Questions(topicId);
        for (String qId : questionIds) {
            approveQuestionUseCase.approveAndWait(qId, QuizUpConstants.ADMIN_USER_ID);
        }

        awaitApprovedQuestionsCounter(topicId, MIN_QUESTIONS_TO_PUBLISH);
        publishTopicUseCase.publishAndWait(topicId, QuizUpConstants.ADMIN_USER_ID);
        logger.info("✓ Pokemon Gen1 topic created and published: {}", topicId);
    }

    private String[] addPokemonGen1Questions(String topicId) {
        String[] ids = new String[20];
        Object[][] questions = {
                {"Combien y a-t-il de Pokémon dans la 1ère génération ?", Map.of(QuestionChoice.A, "150", QuestionChoice.B, "151", QuestionChoice.C, "152", QuestionChoice.D, "149"), QuestionChoice.B},
                {"Quel est le Pokémon de départ de type Feu dans Pokémon Rouge/Bleu ?", Map.of(QuestionChoice.A, "Salamèche", QuestionChoice.B, "Reptincel", QuestionChoice.C, "Dracaufeu", QuestionChoice.D, "Caninos"), QuestionChoice.A},
                {"Quel est le numéro de Pokédex de Mewtwo ?", Map.of(QuestionChoice.A, "149", QuestionChoice.B, "150", QuestionChoice.C, "151", QuestionChoice.D, "148"), QuestionChoice.B},
                {"Quel Pokémon légendaire est le numéro 151 ?", Map.of(QuestionChoice.A, "Mewtwo", QuestionChoice.B, "Artikodin", QuestionChoice.C, "Mew", QuestionChoice.D, "Sulfura"), QuestionChoice.C},
                {"Quelle est l'évolution finale de Carapuce ?", Map.of(QuestionChoice.A, "Carabaffe", QuestionChoice.B, "Tortank", QuestionChoice.C, "Amonistar", QuestionChoice.D, "Racaillou"), QuestionChoice.B},
                {"Quel type est Pikachu ?", Map.of(QuestionChoice.A, "Normal", QuestionChoice.B, "Feu", QuestionChoice.C, "Électrik", QuestionChoice.D, "Vol"), QuestionChoice.C},
                {"Dans quelle ville se trouve la Ligue Pokémon dans la 1ère génération ?", Map.of(QuestionChoice.A, "Parmacia", QuestionChoice.B, "Carmin-sur-Mer", QuestionChoice.C, "Plateau Indigo", QuestionChoice.D, "Lavanville"), QuestionChoice.C},
                {"Quel est le Pokémon fantôme que l'on trouve dans la Tour Pokémon ?", Map.of(QuestionChoice.A, "Spectrum", QuestionChoice.B, "Fantominus", QuestionChoice.C, "Ectoplasma", QuestionChoice.D, "Hypnomade"), QuestionChoice.B},
                {"Quelle pierre fait évoluer Pikachu en Raichu ?", Map.of(QuestionChoice.A, "Pierre Lune", QuestionChoice.B, "Pierre Feu", QuestionChoice.C, "Pierre Tonnerre", QuestionChoice.D, "Pierre Eau"), QuestionChoice.C},
                {"Quel est le premier Pokémon dans l'ordre du Pokédex ?", Map.of(QuestionChoice.A, "Pikachu", QuestionChoice.B, "Salamèche", QuestionChoice.C, "Bulbizarre", QuestionChoice.D, "Carapuce"), QuestionChoice.C},
                {"Quel outil permet de capturer les Pokémon sauvages ?", Map.of(QuestionChoice.A, "Potion", QuestionChoice.B, "Pokéball", QuestionChoice.C, "Rappel", QuestionChoice.D, "Antidote"), QuestionChoice.B},
                {"Quel est le nom du rival du joueur dans Pokémon Rouge/Bleu ?", Map.of(QuestionChoice.A, "Sacha", QuestionChoice.B, "Pierre", QuestionChoice.C, "Gary / Blue", QuestionChoice.D, "Ondine"), QuestionChoice.C},
                {"Combien y a-t-il d'arènes dans la 1ère génération ?", Map.of(QuestionChoice.A, "6", QuestionChoice.B, "7", QuestionChoice.C, "8", QuestionChoice.D, "10"), QuestionChoice.C},
                {"Quel Pokémon de type Normal/Vol est donné au joueur au début de l'aventure ?", Map.of(QuestionChoice.A, "Roucool", QuestionChoice.B, "Doduo", QuestionChoice.C, "Pikachu", QuestionChoice.D, "Rattata"), QuestionChoice.A},
                {"Quel est le type de Osselait ?", Map.of(QuestionChoice.A, "Roche", QuestionChoice.B, "Sol", QuestionChoice.C, "Spectre", QuestionChoice.D, "Normal"), QuestionChoice.D},
                {"Quelle capacité apprend Dracaufeu qui est exclusive à son espèce et de type Vol ?", Map.of(QuestionChoice.A, "Tranche-Aile", QuestionChoice.B, "Aéropique", QuestionChoice.C, "Ronflement", QuestionChoice.D, "Lance-Flammes"), QuestionChoice.B},
                {"Quel est le Pokémon fossile que l'on peut obtenir avec le Dôme Fossile ?", Map.of(QuestionChoice.A, "Amonita", QuestionChoice.B, "Kabuto", QuestionChoice.C, "Ptéra", QuestionChoice.D, "Amonistar"), QuestionChoice.B},
                {"Quel est le type de Magmar ?", Map.of(QuestionChoice.A, "Feu", QuestionChoice.B, "Feu/Vol", QuestionChoice.C, "Feu/Psy", QuestionChoice.D, "Feu/Normal"), QuestionChoice.A},
                {"Dans quelle version exclusive peut-on capturer Mewtwo ?", Map.of(QuestionChoice.A, "Uniquement Pokémon Rouge", QuestionChoice.B, "Uniquement Pokémon Bleu", QuestionChoice.C, "Les deux versions", QuestionChoice.D, "Aucune, il faut l'échanger"), QuestionChoice.C},
                {"Quel est le nom du professeur qui remet le Pokédex au joueur ?", Map.of(QuestionChoice.A, "Professeur Orme", QuestionChoice.B, "Professeur Sorbier", QuestionChoice.C, "Professeur Chen", QuestionChoice.D, "Professeur Sapin"), QuestionChoice.C},
        };

        for (int i = 0; i < questions.length; i++) {
            ids[i] = UUID.randomUUID().toString();
            createQuestionUseCase.createAndWait(
                    ids[i],
                    topicId,
                    (String) questions[i][0],
                    (Map<QuestionChoice, String>) questions[i][1],
                    (QuestionChoice) questions[i][2],
                    QuizUpConstants.ADMIN_USER_ID
            );
        }
        return ids;
    }


    private void seedPokemonGen2Topic() {
        String topicId = "topic-pokemon-gen2-001";

        boolean exists = checkTopicUseCase.existsByIdAndWait(topicId);

        if (exists) {
            logger.info("Pokemon Gen2 topic already exists, skipping creation");
            return;
        }

        logger.info("Creating Pokemon Gen2 topic...");

        createTopicUseCase.createAndWait(
                topicId,
                "Pokémon 2G",
                "Attrapez-les tous ! Testez vos connaissances sur la 2nd génération Pokémon !",
                TopicCategory.GAMES,
                QuizUpConstants.ADMIN_USER_ID
        );

        String[] questionIds = addPokemonGen2Questions(topicId);
        for (String qId : questionIds) {
            approveQuestionUseCase.approveAndWait(qId, QuizUpConstants.ADMIN_USER_ID);
        }

        awaitApprovedQuestionsCounter(topicId, MIN_QUESTIONS_TO_PUBLISH);
        publishTopicUseCase.publishAndWait(topicId, QuizUpConstants.ADMIN_USER_ID);
        logger.info("✓ Pokemon Gen2 topic created and published: {}", topicId);
    }

    private void awaitApprovedQuestionsCounter(String topicId, int requiredApprovedCount) {
        long deadlineMs = System.currentTimeMillis() + 15_000;

        while (System.currentTimeMillis() < deadlineMs) {
            Topic topic = getTopicUseCase.getById(topicId).join();
            int approvedCount = topic.questionsCounter() == null
                    ? 0
                    : topic.questionsCounter().getOrDefault(QuestionStatus.APPROVED, 0);

            if (approvedCount >= requiredApprovedCount) {
                return;
            }

            try {
                Thread.sleep(150);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Interrupted while waiting for counters sync", e);
            }
        }

        throw new IllegalStateException("Timeout while waiting approved questions counter for topic " + topicId);
    }

    private String[] addPokemonGen2Questions(String topicId) {
        String[] ids = new String[20];
        Object[][] questions = {
                {"Combien y a-t-il de nouveaux Pokémon dans la 2ème génération ?", Map.of(QuestionChoice.A, "100", QuestionChoice.B, "251", QuestionChoice.C, "151", QuestionChoice.D, "200"), QuestionChoice.A},
                {"Quel est le Pokémon de départ de type Feu dans Pokémon Or/Argent ?", Map.of(QuestionChoice.A, "Héricendre", QuestionChoice.B, "Feurisson", QuestionChoice.C, "Typhlosion", QuestionChoice.D, "Magby"), QuestionChoice.A},
                {"Quel est le Pokémon légendaire emblématique de Pokémon Or ?", Map.of(QuestionChoice.A, "Lugia", QuestionChoice.B, "Raikou", QuestionChoice.C, "Ho-Oh", QuestionChoice.D, "Suicune"), QuestionChoice.C},
                {"Quel est le Pokémon légendaire emblématique de Pokémon Argent ?", Map.of(QuestionChoice.A, "Ho-Oh", QuestionChoice.B, "Lugia", QuestionChoice.C, "Entei", QuestionChoice.D, "Célébi"), QuestionChoice.B},
                {"Dans quelle région se déroule l'aventure de la 2ème génération ?", Map.of(QuestionChoice.A, "Kanto", QuestionChoice.B, "Hoenn", QuestionChoice.C, "Johto", QuestionChoice.D, "Sinnoh"), QuestionChoice.C},
                {"Quel est le Pokémon de départ de type Eau dans Pokémon Or/Argent ?", Map.of(QuestionChoice.A, "Marill", QuestionChoice.B, "Kaiminus", QuestionChoice.C, "Crocrodil", QuestionChoice.D, "Aligatueur"), QuestionChoice.B},
                {"Quel Pokémon mythique est le numéro 251 ?", Map.of(QuestionChoice.A, "Mew", QuestionChoice.B, "Lugia", QuestionChoice.C, "Célébi", QuestionChoice.D, "Ho-Oh"), QuestionChoice.C},
                {"Quelle nouvelle mécanique a été introduite dans la 2ème génération ?", Map.of(QuestionChoice.A, "Les méga-évolutions", QuestionChoice.B, "La reproduction et les œufs", QuestionChoice.C, "Les capacités Z", QuestionChoice.D, "Les Pokémon chromatiques uniquement"), QuestionChoice.B},
                {"Quel est le nom du trio légendaire de Johto que l'on doit poursuivre ?", Map.of(QuestionChoice.A, "Raikou, Entei, Suicune", QuestionChoice.B, "Artikodin, Électhor, Sulfura", QuestionChoice.C, "Lugia, Ho-Oh, Célébi", QuestionChoice.D, "Raikou, Lugia, Entei"), QuestionChoice.A},
                {"Quelle évolution de Évoli de type Ténèbres est introduite en 2ème génération ?", Map.of(QuestionChoice.A, "Noctali", QuestionChoice.B, "Mentali", QuestionChoice.C, "Givrali", QuestionChoice.D, "Voltali"), QuestionChoice.A},
                {"Quel objet permet à Onix d'évoluer en Steelix ?", Map.of(QuestionChoice.A, "Pierre Tonnerre", QuestionChoice.B, "Manteau Métal", QuestionChoice.C, "Poing de Fer", QuestionChoice.D, "Pierre Acier"), QuestionChoice.B},
                {"Quelle ville de Johto est célèbre pour sa Tour Crampon ?", Map.of(QuestionChoice.A, "Doublonville", QuestionChoice.B, "Ecorcia", QuestionChoice.C, "Ariane", QuestionChoice.D, "Oliville"), QuestionChoice.A},
                {"Quel Pokémon bébé est le pré-évolution de Lippoutou ?", Map.of(QuestionChoice.A, "Toudoudou", QuestionChoice.B, "Mimitoss", QuestionChoice.C, "Pichu", QuestionChoice.D, "Négapi"), QuestionChoice.A},
                {"Combien y a-t-il d'arènes dans la région de Johto ?", Map.of(QuestionChoice.A, "6", QuestionChoice.B, "8", QuestionChoice.C, "10", QuestionChoice.D, "16"), QuestionChoice.B},
                {"Quel est le type d'Aligatueur, l'évolution finale de Kaiminus ?", Map.of(QuestionChoice.A, "Eau", QuestionChoice.B, "Eau/Glace", QuestionChoice.C, "Eau/Combat", QuestionChoice.D, "Eau/Ténèbres"), QuestionChoice.A},
                {"Quel appareil permet d'appeler des dresseurs pour des revanches en 2G ?", Map.of(QuestionChoice.A, "Pokénav", QuestionChoice.B, "Pokégear", QuestionChoice.C, "Pokétch", QuestionChoice.D, "Explorateur"), QuestionChoice.B},
                {"Quel est le professeur Pokémon de la 2ème génération ?", Map.of(QuestionChoice.A, "Professeur Chen", QuestionChoice.B, "Professeur Orme", QuestionChoice.C, "Professeur Sorbier", QuestionChoice.D, "Professeur Cognac"), QuestionChoice.B},
                {"Quelle région peut-on visiter après avoir battu la Ligue Pokémon en 2G ?", Map.of(QuestionChoice.A, "Johto", QuestionChoice.B, "Hoenn", QuestionChoice.C, "Kanto", QuestionChoice.D, "Sinnoh"), QuestionChoice.C},
                {"Quel objet fait évoluer Porygon en Porygon2 ?", Map.of(QuestionChoice.A, "Câble Liaison", QuestionChoice.B, "Disque Amélio", QuestionChoice.C, "Pièce Ronde", QuestionChoice.D, "Manteau Métal"), QuestionChoice.B},
                {"Quel est le nom du rival dans Pokémon Or/Argent ?", Map.of(QuestionChoice.A, "Pierre", QuestionChoice.B, "Silver", QuestionChoice.C, "Gold", QuestionChoice.D, "Kris"), QuestionChoice.B},
        };

        for (int i = 0; i < questions.length; i++) {
            ids[i] = UUID.randomUUID().toString();
            createQuestionUseCase.createAndWait(
                    ids[i],
                    topicId,
                    (String) questions[i][0],
                    (Map<QuestionChoice, String>) questions[i][1],
                    (QuestionChoice) questions[i][2],
                    QuizUpConstants.ADMIN_USER_ID
            );
        }
        return ids;
    }
}
