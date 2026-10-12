package application.engine.model;

import java.util.Map;
import java.util.Objects;

/**
 * Demande de simulation : résultats hypothétiques pour des évaluations sans note.
 *
 * @param semesterId semestre simulé
 * @param hypotheses résultat supposé par évaluation
 */
public record SimulationRequest(SemesterId semesterId, Map<EvaluationId, GradeEntry> hypotheses) {

    public SimulationRequest {
        Objects.requireNonNull(semesterId, "semesterId");
        hypotheses = hypotheses == null ? Map.of() : Map.copyOf(hypotheses);
    }
}
