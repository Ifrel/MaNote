package application.engine.model;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Données saisies pour créer ou modifier un semestre.
 *
 * @param yearId    année universitaire de rattachement
 * @param number    numéro du semestre (≥ 1)
 * @param label     libellé
 * @param startDate début
 * @param endDate   fin
 */
public record SemesterDraft(AcademicYearId yearId, int number, String label, LocalDate startDate, LocalDate endDate) {

    public SemesterDraft {
        Objects.requireNonNull(yearId, "yearId");
        label = label == null ? "" : label.strip();
        Objects.requireNonNull(startDate, "startDate");
        Objects.requireNonNull(endDate, "endDate");
    }
}
