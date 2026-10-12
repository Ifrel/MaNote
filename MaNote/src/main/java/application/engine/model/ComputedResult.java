package application.engine.model;

import java.util.Objects;
import java.util.Optional;

/**
 * Résultat d'un calcul effectué par le moteur (moyenne, note normalisée, contribution…).
 * <p>
 * Le moteur est la seule source de vérité des calculs. L'interface affiche ces résultats
 * sans jamais les recalculer. Un résultat peut ne pas exister : ce type scellé oblige
 * l'interface à traiter explicitement chaque cas.
 *
 * <ul>
 *     <li>{@link Value} : valeur calculée, avec sa {@link ResultNature nature} (définitive, provisoire, simulée) ;</li>
 *     <li>{@link NotComputable} : le calcul est impossible pour l'instant (raison lisible fournie) ;</li>
 *     <li>{@link NotProvided} : le moteur ne fournit pas cet indicateur ;</li>
 *     <li>{@link Failed} : le calcul a échoué (configuration incohérente, erreur interne…).</li>
 * </ul>
 */
public sealed interface ComputedResult
        permits ComputedResult.Value, ComputedResult.NotComputable, ComputedResult.NotProvided, ComputedResult.Failed {

    /**
     * Valeur calculée.
     *
     * @param value       valeur numérique
     * @param outOf       échelle dans laquelle la valeur est exprimée (ex. 20)
     * @param nature      définitive, provisoire ou simulée
     * @param explanation explication courte destinée à l'utilisateur (peut être vide)
     */
    record Value(double value, double outOf, ResultNature nature, String explanation) implements ComputedResult {
        public Value {
            if (!Double.isFinite(value)) {
                throw new IllegalArgumentException("Valeur calculée non finie");
            }
            if (!Double.isFinite(outOf) || outOf <= 0) {
                throw new IllegalArgumentException("Échelle invalide");
            }
            Objects.requireNonNull(nature, "nature");
            explanation = explanation == null ? "" : explanation;
        }
    }

    /**
     * Le calcul n'est pas possible actuellement.
     *
     * @param reason raison lisible (ex. « Aucune note disponible »)
     */
    record NotComputable(String reason) implements ComputedResult {
        public NotComputable {
            reason = reason == null || reason.isBlank() ? "Résultat non calculable" : reason;
        }
    }

    /**
     * Le moteur ne fournit pas cet indicateur.
     */
    record NotProvided() implements ComputedResult {
    }

    /**
     * Le calcul a échoué.
     *
     * @param message description de l'erreur destinée à l'utilisateur
     */
    record Failed(String message) implements ComputedResult {
        public Failed {
            message = message == null || message.isBlank() ? "Erreur de calcul" : message;
        }
    }

    // ------------------------------------------------------------------ fabriques

    static ComputedResult value(double value, double outOf, ResultNature nature, String explanation) {
        return new Value(value, outOf, nature, explanation);
    }

    static ComputedResult notComputable(String reason) {
        return new NotComputable(reason);
    }

    static ComputedResult notProvided() {
        return new NotProvided();
    }

    static ComputedResult failed(String message) {
        return new Failed(message);
    }

    // ------------------------------------------------------------------ accès

    /**
     * @return la valeur si le résultat est calculé, sinon {@code Optional.empty()}
     */
    default Optional<Value> asValue() {
        return this instanceof Value v ? Optional.of(v) : Optional.empty();
    }

    /**
     * @return {@code true} si une valeur calculée est disponible
     */
    default boolean hasValue() {
        return this instanceof Value;
    }
}
