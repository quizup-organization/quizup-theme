package io.github.quizup.theme.infrastructure.config.seed;

/**
 * Erreur de validation d'un fichier de seed YAML (champ manquant, valeur invalide...).
 */
public class SeedDataValidationException extends RuntimeException {

    public SeedDataValidationException(String message) {
        super(message);
    }

    public SeedDataValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
