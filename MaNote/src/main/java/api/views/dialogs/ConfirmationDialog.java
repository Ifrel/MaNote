package api.views.dialogs;

import api.views.app.Theme;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;

import java.util.Optional;

/**
 * Dialogue de confirmation stylisé pour les opérations sensibles.
 */
public final class ConfirmationDialog {

    private ConfirmationDialog() {
    }

    public static boolean confirm(String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);

        Theme.styleDialog(alert.getDialogPane());
        alert.getDialogPane().getStyleClass().add("card");

        Button okBtn = (Button) alert.getDialogPane().lookupButton(ButtonType.OK);
        if (okBtn != null) {
            okBtn.getStyleClass().add("btn-danger");
            okBtn.setText("Confirmer");
        }
        Button cancelBtn = (Button) alert.getDialogPane().lookupButton(ButtonType.CANCEL);
        if (cancelBtn != null) {
            cancelBtn.getStyleClass().add("btn-secondary");
            cancelBtn.setText("Annuler");
        }

        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }
}
