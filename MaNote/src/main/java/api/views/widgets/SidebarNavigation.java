package api.views.widgets;

import api.controllers.AppController;
import api.model.AppContext;
import api.model.NavigationDestination;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.EnumMap;
import java.util.Map;

/**
 * Barre de navigation latérale (SidebarNavigation).
 */
public class SidebarNavigation extends VBox {

    private final AppController controller;
    private final Map<NavigationDestination, Button> navButtons = new EnumMap<>(NavigationDestination.class);

    public SidebarNavigation(AppController controller) {
        this.controller = controller;
        AppContext context = controller.getContext();

        getStyleClass().add("sidebar");
        setPrefWidth(240);
        setMinWidth(220);
        setMaxWidth(260);
        setSpacing(6);

        // Logo & Titre de l'application
        HBox brandBox = new HBox(12);
        brandBox.setAlignment(Pos.CENTER_LEFT);
        brandBox.setStyle("-fx-padding: 0 0 24px 8px;");

        FontIcon brandIcon = new FontIcon(Feather.AWARD);
        brandIcon.setIconSize(24);
        brandIcon.getStyleClass().add("text-accent");

        VBox brandText = new VBox(2);
        Label appName = new Label("MaNote");
        appName.getStyleClass().add("heading-md");
        Label appSubtitle = new Label("Gestion Académique");
        appSubtitle.getStyleClass().add("text-tertiary");
        appSubtitle.setStyle("-fx-font-size: 11px;");

        brandText.getChildren().addAll(appName, appSubtitle);
        brandBox.getChildren().addAll(brandIcon, brandText);
        getChildren().add(brandBox);

        // Items de navigation principaux
        for (NavigationDestination dest : NavigationDestination.values()) {
            // Ignorer UE_DETAILS dans la barre directe car on y accède en cliquant sur une UE
            if (dest == NavigationDestination.UE_DETAILS) continue;

            Button btn = createNavButton(dest);
            navButtons.put(dest, btn);
            getChildren().add(btn);
        }

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);
        getChildren().add(spacer);

        // Pied de navigation : Année active & Statut
        VBox footerBox = new VBox(6);
        footerBox.setStyle("-fx-padding: 12px 8px 0 8px; -fx-border-color: -fx-border-subtle transparent transparent transparent; -fx-border-width: 1px;");

        Label yearLabel = new Label("Année en cours");
        yearLabel.getStyleClass().add("text-tertiary");
        yearLabel.setStyle("-fx-font-size: 10px;");

        Label yearValue = new Label();
        yearValue.getStyleClass().add("heading-sm");
        yearValue.textProperty().bind(context.activeYearProperty().map(y -> y != null ? y.label() : "—"));

        footerBox.getChildren().addAll(yearLabel, yearValue);
        getChildren().add(footerBox);

        // Observer les changements de destination
        context.currentDestinationProperty().addListener((obs, oldV, newV) -> updateActiveState(newV));
        updateActiveState(context.getCurrentDestination());
    }

    private Button createNavButton(NavigationDestination dest) {
        Button btn = new Button(dest.getTitle());
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setAlignment(Pos.CENTER_LEFT);

        Feather iconCode = Feather.valueOf(dest.getIconName());
        FontIcon icon = new FontIcon(iconCode);
        icon.setIconSize(16);
        btn.setGraphic(icon);
        btn.setGraphicTextGap(12);

        btn.setOnAction(e -> controller.navigateTo(dest));
        return btn;
    }

    private void updateActiveState(NavigationDestination current) {
        for (Map.Entry<NavigationDestination, Button> entry : navButtons.entrySet()) {
            Button btn = entry.getValue();
            boolean active = entry.getKey() == current;
            btn.getStyleClass().removeAll("nav-item", "nav-item-active");
            if (active) {
                btn.getStyleClass().add("nav-item-active");
            } else {
                btn.getStyleClass().add("nav-item");
            }
        }
    }
}
