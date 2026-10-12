package application.engine.model;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalDouble;

/**
 * Résumé d'une évaluation.
 * <p>
 * Invariant vérifié à la construction : {@code grade} est présent si et seulement si
 * {@code status == GRADED}. Une note manquante n'est donc jamais confondue avec zéro.
 *
 * @param id              identifiant
 * @param title           intitulé
 * @param type            type d'évaluation
 * @param ue              UE de rattachement
 * @param semesterId      semestre de rattachement
 * @param date            date prévue ou effective (vide si non encore fixée)
 * @param status          statut
 * @param maxPoints       barème (note maximale)
 * @param weight          coefficient ou poids, si le moteur le gère
 * @param grade           résultat, présent uniquement si {@code status == GRADED}
 * @param normalizedScore résultat converti par le moteur dans l'échelle d'affichage (ex. /20)
 */
public record EvaluationSummary(
        EvaluationId id,
        String title,
        EvaluationType type,
        UeRef ue,
        SemesterId semesterId,
        Optional<LocalDate> date,
        EvaluationStatus status,
        double maxPoints,
        OptionalDouble weight,
        Optional<GradeEntry> grade,
        ComputedResult normalizedScore) {

    public EvaluationSummary {
        Objects.requireNonNull(id, "id");
        title = title == null ? "" : title;
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(ue, "ue");
        Objects.requireNonNull(semesterId, "semesterId");
        date = date == null ? Optional.empty() : date;
        Objects.requireNonNull(status, "status");
        weight = weight == null ? OptionalDouble.empty() : weight;
        grade = grade == null ? Optional.empty() : grade;
        Objects.requireNonNull(normalizedScore, "normalizedScore");
        if (grade.isPresent() != (status == EvaluationStatus.GRADED)) {
            throw new IllegalArgumentException(
                    "Invariant violé : une note doit être présente si et seulement si le statut est GRADED (" + id + ")");
        }
    }
}
