package api.views.widgets;

import api.controllers.AppController;
import api.model.AppContext;
import api.model.NavigationDestination;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.EnumMap;
import java.util.Map;

/**
 * Barre de navigation latérale (encre, accent lumineux sur l'entrée active).
 */
public class SidebarNavigation extends VBox {

    private final AppController controller;
    private final Map<NavigationDestination, Button> navButtons = new EnumMap<>(NavigationDestination.class);

    public SidebarNavigation(AppController controller) {
        this.controller = controller;
        AppContext context = controller.getContext();

        getStyleClass().add("sidebar");
        setPrefWidth(244);
        setMinWidth(224);
        setMaxWidth(264);
        setSpacing(4);

        // Marque
        HBox brandBox = new HBox(12);
        brandBox.setAlignment(Pos.CENTER_LEFT);
        brandBox.setPadding(new Insets(0, 0, 26, 6));

        FontIcon brandIcon = new FontIcon(Feather.AWARD);
        brandIcon.setIconSize(20);
        StackPane chip = new StackPane(brandIcon);
        chip.getStyleClass().add("brand-chip");

        VBox brandText = new VBox(1);
        Label appName = new Label("MaNote");
        appName.getStyleClass().add("heading-md");
        Label appSubtitle = new Label("Gestion académique");
        appSubtitle.getStyleClass().add("sidebar-caption");
        brandText.getChildren().addAll(appName, appSubtitle);
        brandBox.getChildren().addAll(chip, brandText);
        getChildren().add(brandBox);

        // Navigation (le détail d'UE s'ouvre depuis une UE, pas depuis la barre)
        for (NavigationDestination dest : NavigationDestination.values()) {
            if (dest == NavigationDestination.UE_DETAILS) continue;
            Button btn = createNavButton(dest);
            navButtons.put(dest, btn);
            getChildren().add(btn);
        }

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);
        getChildren().add(spacer);

        // Pied : année en cours
        VBox footerBox = new VBox(3);
        footerBox.getStyleClass().add("sidebar-footer");
        Label yearLabel = new Label("Année en cours");
        yearLabel.getStyleClass().add("sidebar-caption");
        Label yearValue = new Label();
        yearValue.getStyleClass().add("heading-sm");
        yearValue.textProperty().bind(context.activeYearProperty().map(y -> y != null ? y.label() : "—"));
        footerBox.getChildren().addAll(yearLabel, yearValue);
        getChildren().add(footerBox);

        context.currentDestinationProperty().addListener((obs, oldV, newV) -> updateActiveState(newV));
        updateActiveState(context.getCurrentDestination());
    }

    private Button createNavButton(NavigationDestination dest) {
        Button btn = new Button(dest.getTitle());
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setAlignment(Pos.CENTER_LEFT);

        FontIcon icon = new FontIcon(Feather.valueOf(dest.getIconName()));
        icon.setIconSize(17);
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
            btn.getStyleClass().add(active ? "nav-item-active" : "nav-item");
        }
    }
}
