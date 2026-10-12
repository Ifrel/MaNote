package application.engine.model;

import java.util.Objects;
import java.util.Optional;

/**
 * Résumé d'une unité d'enseignement (UE).
 *
 * @param id         identifiant
 * @param semesterId semestre de rattachement
 * @param code       code (ex. « INF701 »)
 * @param name       intitulé
 * @param credits    crédits ECTS
 * @param average    moyenne de l'UE calculée par le moteur
 * @param validation information de validation de l'UE
 * @param progress   décompte des évaluations de l'UE
 * @param attention  raison pour laquelle l'UE nécessite une attention particulière, déterminée par le moteur
 */
public record UeSummary(
        UeId id,
        SemesterId semesterId,
        String code,
        String name,
        int credits,
        ComputedResult average,
        ValidationInfo validation,
        EvaluationProgress progress,
        Optional<String> attention) {

    public UeSummary {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(semesterId, "semesterId");
        code = code == null ? "" : code;
        name = name == null ? "" : name;
        Objects.requireNonNull(average, "average");
        Objects.requireNonNull(validation, "validation");
        Objects.requireNonNull(progress, "progress");
        attention = attention == null ? Optional.empty() : attention;
    }

    /**
     * @return une référence légère vers cette UE
     */
    public UeRef ref() {
        return new UeRef(id, code, name);
    }
}
