package io.github.quizup.theme.domain.port.out;

import io.github.quizup.theme.domain.model.ImageReferenceCount;

import java.util.List;

/**
 * Port sortant — comptage des références d'images dans le read model theme
 * (couverture/bannière de sujet, illustration de question), pour la console
 * d'administration des médias du BFF.
 */
public interface ImageReferenceRepositoryPort {

    /**
     * Compte, pour chaque URL demandée, le nombre de références existantes
     * (tous statuts confondus). Les URLs absentes du read model ne sont pas retournées.
     *
     * @param imageUrls URLs recherchées (liste vide = aucun résultat)
     * @return un compteur par URL trouvée
     */
    List<ImageReferenceCount> countByImageUrls(List<String> imageUrls);
}
