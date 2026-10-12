package application.engine.config;

import java.util.List;
import java.util.OptionalDouble;

/**
 * Contraintes de saisie d'un paramètre, utilisées par l'interface pour construire l'éditeur adapté
 * et effectuer une première validation (le moteur reste responsable de la validation finale).
 *
 * @param min     valeur minimale (paramètres numériques)
 * @param max     valeur maximale (paramètres numériques)
 * @param step    pas de saisie (paramètres numériques)
 * @param unit    unité affichée (ex. « /20 », « % »), peut être vide
 * @param choices options disponibles (paramètres de type choix)
 */
public record ParameterConstraints(
        OptionalDouble min,
        OptionalDouble max,
        OptionalDouble step,
        String unit,
        List<Choice> choices) {

    public ParameterConstraints {
        min = min == null ? OptionalDouble.empty() : min;
        max = max == null ? OptionalDouble.empty() : max;
        step = step == null ? OptionalDouble.empty() : step;
        unit = unit == null ? "" : unit;
        choices = choices == null ? List.of() : List.copyOf(choices);
    }

    public static ParameterConstraints none() {
        return new ParameterConstraints(OptionalDouble.empty(), OptionalDouble.empty(), OptionalDouble.empty(), "", List.of());
    }

    public static ParameterConstraints number(double min, double max, double step, String unit) {
        return new ParameterConstraints(OptionalDouble.of(min), OptionalDouble.of(max), OptionalDouble.of(step), unit, List.of());
    }

    public static ParameterConstraints choices(List<Choice> choices) {
        return new ParameterConstraints(OptionalDouble.empty(), OptionalDouble.empty(), OptionalDouble.empty(), "", choices);
    }
}
