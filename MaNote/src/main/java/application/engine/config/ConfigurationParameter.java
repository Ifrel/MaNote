package application.engine.config;

import java.util.Objects;
import java.util.Optional;

/**
 * Paramètre de calcul exposé par le moteur.
 *
 * @param key           clé stable utilisée lors de la mise à jour
 * @param label         libellé affiché
 * @param description   explication de l'effet du paramètre
 * @param value         valeur effective dans le périmètre consulté
 * @param constraints   contraintes de saisie
 * @param editable      {@code false} si le paramètre est en lecture seule
 * @param inheritedFrom si la valeur est héritée d'un périmètre parent, libellé de ce périmètre
 */
public record ConfigurationParameter(
        String key,
        String label,
        String description,
        ConfigValue value,
        ParameterConstraints constraints,
        boolean editable,
        Optional<String> inheritedFrom) {

    public ConfigurationParameter {
        Objects.requireNonNull(key, "key");
        label = label == null || label.isBlank() ? key : label;
        description = description == null ? "" : description;
        Objects.requireNonNull(value, "value");
        constraints = constraints == null ? ParameterConstraints.none() : constraints;
        inheritedFrom = inheritedFrom == null ? Optional.empty() : inheritedFrom;
    }
}
