package application.engine.model;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Résultat d'une simulation. Les valeurs retournées doivent porter la nature
 * {@link ResultNature#SIMULATED} : elles ne sont jamais des résultats obtenus.
 *
 * @param semesterAverage moyenne simulée du semestre
 * @param ueAverages      moyennes simulées par UE
 * @param ueValidations   validations simulées par UE
 * @param assumptions     hypothèses retenues par le moteur, affichées à l'utilisateur
 */
public record SimulationResult(
        ComputedResult semesterAverage,
        Map<UeId, ComputedResult> ueAverages,
        Map<UeId, ValidationInfo> ueValidations,
        List<String> assumptions) {

    public SimulationResult {
        Objects.requireNonNull(semesterAverage, "semesterAverage");
        ueAverages = ueAverages == null ? Map.of() : Map.copyOf(ueAverages);
        ueValidations = ueValidations == null ? Map.of() : Map.copyOf(ueValidations);
        assumptions = assumptions == null ? List.of() : List.copyOf(assumptions);
    }
}
