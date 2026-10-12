package application.engine.model;

import java.util.Objects;
import java.util.OptionalDouble;

/**
 * Contribution d'une évaluation au résultat de son UE, telle que calculée par le moteur.
 *
 * @param evaluationId évaluation concernée
 * @param title        intitulé de l'évaluation
 * @param weightShare  part relative (0..1) de l'évaluation dans le calcul, si le moteur la fournit
 * @param contribution points apportés au résultat de l'UE
 */
public record EvaluationContribution(
        EvaluationId evaluationId,
        String title,
        OptionalDouble weightShare,
        ComputedResult contribution) {

    public EvaluationContribution {
        Objects.requireNonNull(evaluationId, "evaluationId");
        title = title == null ? "" : title;
        weightShare = weightShare == null ? OptionalDouble.empty() : weightShare;
        Objects.requireNonNull(contribution, "contribution");
    }
}
