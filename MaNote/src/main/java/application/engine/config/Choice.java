package application.engine.config;

import java.util.Objects;

/**
 * Option proposée pour un paramètre de type choix.
 *
 * @param code        code stable
 * @param label       libellé affiché
 * @param description explication facultative
 */
public record Choice(String code, String label, String description) {

    public Choice {
        Objects.requireNonNull(code, "code");
        label = label == null || label.isBlank() ? code : label;
        description = description == null ? "" : description;
    }

    public Choice(String code, String label) {
        this(code, label, "");
    }
}
