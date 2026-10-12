package application.engine.model;

import java.util.Objects;

/**
 * Information de validation fournie par le moteur.
 *
 * @param state  état de validation
 * @param label  libellé court affiché dans un badge (ex. « Validée »)
 * @param detail explication destinée à l'utilisateur (peut être vide)
 */
public record ValidationInfo(ValidationState state, String label, String detail) {

    public ValidationInfo {
        Objects.requireNonNull(state, "state");
        label = label == null ? "" : label;
        detail = detail == null ? "" : detail;
    }

    /**
     * Aucune information de validation n'est fournie par le moteur.
     */
    public static ValidationInfo notProvided() {
        return new ValidationInfo(ValidationState.NOT_PROVIDED, "Non communiqué", "");
    }

    public static ValidationInfo of(ValidationState state, String label, String detail) {
        return new ValidationInfo(state, label, detail);
    }
}
