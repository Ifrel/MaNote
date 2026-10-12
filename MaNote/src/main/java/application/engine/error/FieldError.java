package application.engine.error;

import java.util.Objects;

/**
 * Erreur de validation rattachée à un champ.
 *
 * @param field   nom du champ (ex. {@code "title"}, {@code "maxPoints"}, ou clé de paramètre de configuration) ;
 *                {@code ""} pour une erreur globale
 * @param message message destiné à l'utilisateur
 */
public record FieldError(String field, String message) {

    public FieldError {
        field = field == null ? "" : field;
        Objects.requireNonNull(message, "message");
    }
}
