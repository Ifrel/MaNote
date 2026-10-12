package application.engine.model;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;

/**
 * Détail complet d'une évaluation.
 *
 * @param summary         résumé
 * @param description     informations complémentaires (peut être vide)
 * @param gradeRecordedOn date d'enregistrement du résultat, si connue
 * @param contribution    contribution de l'évaluation au résultat de l'UE, calculée par le moteur
 */
public record EvaluationDetails(
        EvaluationSummary summary,
        String description,
        Optional<LocalDate> gradeRecordedOn,
        ComputedResult contribution) {

    public EvaluationDetails {
        Objects.requireNonNull(summary, "summary");
        description = description == null ? "" : description;
        gradeRecordedOn = gradeRecordedOn == null ? Optional.empty() : gradeRecordedOn;
        Objects.requireNonNull(contribution, "contribution");
    }
}
