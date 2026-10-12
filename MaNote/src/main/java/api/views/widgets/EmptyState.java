package api.views.widgets;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;

/**
 * Composant d'état vide réutilisable lorsqu'aucune donnée n'est disponible.
 */
public class EmptyState extends VBox {

    public EmptyState(Feather icon, String title, String description, String actionText, Runnable onAction) {
        setAlignment(Pos.CENTER);
        setSpacing(12);
        setStyle("-fx-padding: 40px 20px;");

        if (icon != null) {
            FontIcon fontIcon = new FontIcon(icon);
            fontIcon.setIconSize(48);
            fontIcon.getStyleClass().add("text-tertiary");
            getChildren().add(fontIcon);
        }

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("heading-md");

        Label descLabel = new Label(description);
        descLabel.getStyleClass().add("text-muted");
        descLabel.setWrapText(true);
        descLabel.setMaxWidth(400);
        descLabel.setAlignment(Pos.CENTER);

        getChildren().addAll(titleLabel, descLabel);

        if (actionText != null && onAction != null) {
            Button actionButton = new Button(actionText);
            actionButton.getStyleClass().add("btn-primary");
            actionButton.setOnAction(e -> onAction.run());
            getChildren().add(actionButton);
        }
    }
}
