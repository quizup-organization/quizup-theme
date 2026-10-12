package io.github.quizup.theme.domain.model;

import io.github.quizup.microservice.core.domain.model.i18n.Language;

import java.util.Set;

/**
 * Couverture linguistique d'un thème : nombre de questions approuvées disponibles dans
 * <b>toutes</b> les langues de {@code languages} (sémantique stricte, identique au comptage
 * utilisé par les gardes de disponibilité). La façade prépare cette photo à la création d'un
 * ticket d'appariement pour que les événements consommés par la saga soient auto-suffisants
 * (aucune query bloquante dans un handler de saga).
 */
public record QuestionLanguageCoverage(
        Set<Language> languages,
        int approvedCount
) {
}
