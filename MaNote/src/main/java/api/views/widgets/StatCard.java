package api.views.widgets;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;

/**
 * Carte statistique réutilisable affichant un indicateur clé (KPI).
 */
public class StatCard extends VBox {

    private final Label titleLabel = new Label();
    private final Label valueLabel = new Label();
    private final Label subtitleLabel = new Label();
    private final HBox headerBox = new HBox(8);

    public StatCard(String title, String value, String subtitle, Feather icon) {
        getStyleClass().add("stat-card");
        setSpacing(8);

        titleLabel.setText(title);
        titleLabel.getStyleClass().add("stat-label");

        valueLabel.setText(value);
        valueLabel.getStyleClass().add("stat-value");

        subtitleLabel.setText(subtitle);
        subtitleLabel.getStyleClass().addAll("text-muted");
        subtitleLabel.setStyle("-fx-font-size: 11px;");

        headerBox.setAlignment(Pos.CENTER_LEFT);
        headerBox.getChildren().add(titleLabel);

        if (icon != null) {
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            FontIcon fontIcon = new FontIcon(icon);
            fontIcon.setIconSize(16);
            fontIcon.getStyleClass().add("text-tertiary");
            headerBox.getChildren().addAll(spacer, fontIcon);
        }

        getChildren().addAll(headerBox, valueLabel, subtitleLabel);
    }

    public void setValue(String value) {
        valueLabel.setText(value);
    }

    public void setSubtitle(String subtitle) {
        subtitleLabel.setText(subtitle);
    }

    public void setBadge(Node badge) {
        if (badge != null) {
            headerBox.getChildren().add(badge);
        }
    }
}
