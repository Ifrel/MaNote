package application.engine.model;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Résumé d'un semestre.
 *
 * @param id           identifiant
 * @param yearId       année universitaire de rattachement
 * @param number       numéro du semestre dans la formation (ex. 1 pour « S1 »)
 * @param label        libellé (ex. « Semestre 1 »)
 * @param startDate    début
 * @param endDate      fin
 * @param state        à venir, en cours ou terminé
 * @param ueCount      nombre d'UE
 * @param totalCredits crédits ECTS cumulés des UE
 * @param average      moyenne du semestre calculée par le moteur
 * @param validation   information de validation du semestre
 * @param progress     décompte des évaluations du semestre
 */
public record SemesterSummary(
        SemesterId id,
        AcademicYearId yearId,
        int number,
        String label,
        LocalDate startDate,
        LocalDate endDate,
        SemesterState state,
        int ueCount,
        int totalCredits,
        ComputedResult average,
        ValidationInfo validation,
        EvaluationProgress progress) {

    public SemesterSummary {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(yearId, "yearId");
        Objects.requireNonNull(label, "label");
        Objects.requireNonNull(startDate, "startDate");
        Objects.requireNonNull(endDate, "endDate");
        Objects.requireNonNull(state, "state");
        Objects.requireNonNull(average, "average");
        Objects.requireNonNull(validation, "validation");
        Objects.requireNonNull(progress, "progress");
    }
}
