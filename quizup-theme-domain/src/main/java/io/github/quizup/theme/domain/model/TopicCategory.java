package io.github.quizup.theme.domain.model;

public enum TopicCategory {
    ARTS("Arts & lettres"),
    BUSINESS("Économie & business"),
    EDUCATION("Éducation"),
    ENTERTAINMENT("Divertissement"),
    FOOD_AND_DRINK("Cuisine & boissons"),
    GAMES("Jeux"),
    GENERAL("Général"),
    HISTORY("Histoire"),
    LITERATURE("Littérature"),
    MOVIES("Cinéma"),
    MUSIC("Musique"),
    NATURE("Nature & animaux"),
    SCIENCE("Sciences"),
    SPORTS("Sport"),
    TELEVISION("Séries & TV"),
    TECHNOLOGY("Technologie"),
    WORLD("Monde & géographie");

    private final String label;

    TopicCategory(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}

