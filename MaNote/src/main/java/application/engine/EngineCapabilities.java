package application.engine;

import application.engine.config.ConfigurationScope;

import java.util.EnumSet;
import java.util.Set;

/**
 * Fonctionnalités facultatives prises en charge par le moteur. L'interface n'affiche que les actions
 * réellement disponibles et présente un état explicite « non disponible » pour les autres.
 *
 * @param structureEditing    création / modification d'années, de semestres et d'UE
 * @param evaluationWeights   les évaluations ont un coefficient ou un poids saisissable
 * @param contributions       le moteur fournit la contribution de chaque évaluation au résultat de l'UE
 * @param averageTimeline     le moteur fournit l'évolution de la moyenne du semestre dans le temps
 * @param simulation          le moteur sait simuler des résultats à partir d'hypothèses
 * @param configurationScopes périmètres de configuration du calcul pris en charge (vide = non configurable)
 */
public record EngineCapabilities(
        boolean structureEditing,
        boolean evaluationWeights,
        boolean contributions,
        boolean averageTimeline,
        boolean simulation,
        Set<ConfigurationScope.Kind> configurationScopes) {

    public EngineCapabilities {
        configurationScopes = configurationScopes == null || configurationScopes.isEmpty()
                ? Set.of()
                : Set.copyOf(EnumSet.copyOf(configurationScopes));
    }

    /**
     * @return {@code true} si au moins un périmètre de configuration est pris en charge
     */
    public boolean configuration() {
        return !configurationScopes.isEmpty();
    }

    /**
     * Capacités minimales : lecture et gestion des évaluations et des notes uniquement.
     */
    public static EngineCapabilities minimal() {
        return new EngineCapabilities(false, false, false, false, false, Set.of());
    }
}
