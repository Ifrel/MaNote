package api.views.widgets;

import api.views.format.Formatters;
import application.engine.model.EvaluationStatus;
import application.engine.model.ValidationInfo;
import application.engine.model.ValidationState;
import javafx.scene.control.Label;

/**
 * Badge de statut réutilisable pour afficher l'état d'une évaluation ou d'une validation.
 */
public class StatusBadge extends Label {

    public StatusBadge(String text, String badgeClass) {
        super(text);
        getStyleClass().addAll("badge", badgeClass);
    }

    public static StatusBadge ofEvaluationStatus(EvaluationStatus status) {
        String text = Formatters.formatEvaluationStatus(status);
        String css = Formatters.getEvaluationStatusBadgeClass(status);
        return new StatusBadge(text, css);
    }

    public static StatusBadge ofValidation(ValidationInfo validation) {
        String text = validation.label().isBlank() ? validation.state().name() : validation.label();
        String css = Formatters.getValidationBadgeClass(validation.state());
        return new StatusBadge(text, css);
    }

    public static StatusBadge ofValidationState(ValidationState state) {
        String css = Formatters.getValidationBadgeClass(state);
        return new StatusBadge(state.name(), css);
    }

    public static StatusBadge neutral(String text) {
        return new StatusBadge(text, "badge-neutral");
    }

    public static StatusBadge success(String text) {
        return new StatusBadge(text, "badge-success");
    }

    public static StatusBadge warning(String text) {
        return new StatusBadge(text, "badge-warning");
    }

    public static StatusBadge danger(String text) {
        return new StatusBadge(text, "badge-danger");
    }

    public static StatusBadge info(String text) {
        return new StatusBadge(text, "badge-info");
    }
}
