package application.engine.config;

import java.util.List;
import java.util.Objects;

/**
 * Configuration du calcul pour un périmètre, <strong>auto-décrite par le moteur</strong>.
 * <p>
 * L'interface génère le formulaire à partir de cette description : elle ne connaît ni les règles
 * universitaires, ni la signification des paramètres. Aucun langage de formules n'est imposé.
 *
 * @param scope       périmètre consulté
 * @param title       titre (ex. « Semestre 1 — 2026–2027 »)
 * @param description explication générale (peut être vide)
 * @param sections    sections de paramètres
 */
public record ConfigurationView(ConfigurationScope scope, String title, String description,
                                List<ConfigurationSection> sections) {

    public ConfigurationView {
        Objects.requireNonNull(scope, "scope");
        title = title == null ? "" : title;
        description = description == null ? "" : description;
        sections = sections == null ? List.of() : List.copyOf(sections);
    }
}
