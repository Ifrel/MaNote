package application.engine.model;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Données saisies pour créer ou modifier une année universitaire.
 *
 * @param label     libellé (ex. « 2027–2028 »)
 * @param programme formation suivie
 * @param startDate date de début
 * @param endDate   date de fin
 */
public record AcademicYearDraft(String label, String programme, LocalDate startDate, LocalDate endDate) {

    public AcademicYearDraft {
        label = label == null ? "" : label.strip();
        programme = programme == null ? "" : programme.strip();
        Objects.requireNonNull(startDate, "startDate");
        Objects.requireNonNull(endDate, "endDate");
    }
}
