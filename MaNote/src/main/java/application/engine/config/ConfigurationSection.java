package application.engine.config;

import java.util.List;
import java.util.Objects;

/**
 * Groupe cohérent de paramètres (ex. « Calcul de la moyenne d'UE », « Validation »).
 *
 * @param id          identifiant stable
 * @param title       titre affiché
 * @param description explication facultative
 * @param parameters  paramètres de la section
 */
public record ConfigurationSection(String id, String title, String description,
                                   List<ConfigurationParameter> parameters) {

    public ConfigurationSection {
        Objects.requireNonNull(id, "id");
        title = title == null ? id : title;
        description = description == null ? "" : description;
        parameters = parameters == null ? List.of() : List.copyOf(parameters);
    }
}
