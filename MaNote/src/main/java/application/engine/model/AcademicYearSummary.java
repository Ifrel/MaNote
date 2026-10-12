package application.engine.model;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Résumé d'une année universitaire.
 *
 * @param id         identifiant
 * @param label      libellé (ex. « 2026–2027 »)
 * @param programme  formation suivie (ex. « Master 1 Informatique »)
 * @param startDate  début de l'année
 * @param endDate    fin de l'année
 * @param current    {@code true} pour l'année en cours
 * @param average    moyenne annuelle calculée par le moteur
 * @param validation information de validation annuelle
 */
public record AcademicYearSummary(
        AcademicYearId id,
        String label,
        String programme,
        LocalDate startDate,
        LocalDate endDate,
        boolean current,
        ComputedResult average,
        ValidationInfo validation) {

    public AcademicYearSummary {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(label, "label");
        programme = programme == null ? "" : programme;
        Objects.requireNonNull(startDate, "startDate");
        Objects.requireNonNull(endDate, "endDate");
        Objects.requireNonNull(average, "average");
        Objects.requireNonNull(validation, "validation");
    }
}
