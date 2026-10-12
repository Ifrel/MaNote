package application.engine.model;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Vue détaillée d'une UE.
 *
 * @param summary          résumé (moyenne, validation, progression)
 * @param description      description facultative
 * @param evaluations      évaluations de l'UE
 * @param contributions    contributions des évaluations au résultat, uniquement si le moteur les fournit
 * @param calculationNotes explications du moteur sur la méthode de calcul appliquée (affichées telles quelles)
 */
public record UeDetails(
        UeSummary summary,
        String description,
        List<EvaluationSummary> evaluations,
        Optional<List<EvaluationContribution>> contributions,
        List<String> calculationNotes) {

    public UeDetails {
        Objects.requireNonNull(summary, "summary");
        description = description == null ? "" : description;
        evaluations = evaluations == null ? List.of() : List.copyOf(evaluations);
        contributions = contributions == null ? Optional.empty() : contributions.map(List::copyOf);
        calculationNotes = calculationNotes == null ? List.of() : List.copyOf(calculationNotes);
    }
}
