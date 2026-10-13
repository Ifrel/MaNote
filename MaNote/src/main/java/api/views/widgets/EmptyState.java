package api.views.widgets;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;

/**
 * État vide : une invitation à agir plutôt qu'un simple message d'absence.
 */
public class EmptyState extends VBox {

    public EmptyState(Feather icon, String title, String description, String actionText, Runnable onAction) {
        setAlignment(Pos.CENTER);
        setSpacing(12);
        setStyle("-fx-padding: 56px 20px;");

        if (icon != null) {
            FontIcon fontIcon = new FontIcon(icon);
            fontIcon.setIconSize(40);
            StackPane circle = new StackPane(fontIcon);
            circle.getStyleClass().add("empty-icon");
            getChildren().add(circle);
        }

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("heading-lg");

        Label descLabel = new Label(description);
        descLabel.getStyleClass().add("text-muted");
        descLabel.setWrapText(true);
        descLabel.setMaxWidth(420);
        descLabel.setAlignment(Pos.CENTER);
        descLabel.setTextAlignment(TextAlignment.CENTER);

        getChildren().addAll(titleLabel, descLabel);

        if (actionText != null && onAction != null) {
            Button actionButton = new Button(actionText);
            actionButton.getStyleClass().add("btn-primary");
            actionButton.setOnAction(e -> onAction.run());
            VBox.setMargin(actionButton, new javafx.geometry.Insets(8, 0, 0, 0));
            getChildren().add(actionButton);
        }
    }
}
