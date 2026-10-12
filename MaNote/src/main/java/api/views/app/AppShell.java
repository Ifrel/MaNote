package api.views.app;

import api.controllers.AppController;
import api.model.AppContext;
import api.model.NavigationDestination;
import api.views.dialogs.EvaluationDialog;
import api.views.screens.*;
import api.views.widgets.SidebarNavigation;
import api.views.widgets.TopBar;
import application.engine.model.SemesterSummary;
import infrastructure.preferences.UserPreferences;
import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.util.EnumMap;
import java.util.Map;

/**
 * Coquille principale de l'application (AppShell).
 * Assemble la Sidebar, la TopBar, la zone centrale et le gestionnaire de notifications toast.
 */
public class AppShell extends BorderPane {

    private final AppController controller;
    private final TopBar topBar;
    private final SidebarNavigation sidebar;
    private final StackPane centerContainer = new StackPane();
    private final VBox toastContainer = new VBox(8);

    private final Map<NavigationDestination, Node> screens = new EnumMap<>(NavigationDestination.class);

    public AppShell(AppController controller) {
        this.controller = controller;
        AppContext context = controller.getContext();

        // 1. Sidebar à gauche
        sidebar = new SidebarNavigation(controller);
        setLeft(sidebar);

        // 2. TopBar en haut
        topBar = new TopBar(controller, this::openNewEvaluationDialog);

        // 3. Zone principale au centre
        VBox mainBox = new VBox();
        VBox.setVgrow(centerContainer, Priority.ALWAYS);
        mainBox.getChildren().addAll(topBar, centerContainer);

        // StackPane pour superposer les toasts au-dessus du contenu
        StackPane rootLayer = new StackPane();
        rootLayer.getChildren().add(mainBox);

        // Toast Container en haut à droite
        toastContainer.setAlignment(Pos.TOP_RIGHT);
        toastContainer.setPadding(new Insets(16, 24, 0, 0));
        toastContainer.setPickOnBounds(false);
        rootLayer.getChildren().add(toastContainer);

        setCenter(rootLayer);

        // 4. Initialisation des écrans
        initScreens();

        // 5. Liaisons
        context.currentDestinationProperty().addListener((obs, oldV, newV) -> showScreen(newV));
        context.toastMessageProperty().addListener((obs, oldV, newV) -> {
            if (newV != null && !newV.isBlank()) {
                displayToast(newV, context.toastTypeProperty().get());
            }
        });

        // Mise à jour de la liste des semestres dans la topbar
        context.activeYearProperty().addListener((obs, oldV, newV) -> {
            if (newV != null) {
                topBar.updateSemesters(controller.getEngine().getSemesters(newV.id()));
            }
        });
        if (context.getActiveYear() != null) {
            topBar.updateSemesters(controller.getEngine().getSemesters(context.getActiveYear().id()));
        }

        // Appliquer l'écran initial
        showScreen(context.getCurrentDestination());
    }

    private void initScreens() {
        screens.put(NavigationDestination.DASHBOARD, new DashboardScreen(controller));
        screens.put(NavigationDestination.SEMESTERS, new SemestersScreen(controller));
        screens.put(NavigationDestination.UE_DETAILS, new UeDetailsScreen(controller));
        screens.put(NavigationDestination.EVALUATIONS, new EvaluationsScreen(controller));
        screens.put(NavigationDestination.ANALYTICS, new AnalyticsScreen(controller));
        screens.put(NavigationDestination.PLANNING, new PlanningScreen(controller));
        screens.put(NavigationDestination.ENGINE_CONFIG, new EngineConfigScreen(controller));
        screens.put(NavigationDestination.SETTINGS, new SettingsScreen(controller));
    }

    private void showScreen(NavigationDestination destination) {
        if (destination == null) return;
        Node screen = screens.get(destination);
        if (screen == null) return;

        // Rafraîchir l'écran si nécessaire
        if (screen instanceof DashboardScreen ds) ds.refresh();
        else if (screen instanceof SemestersScreen ss) ss.refresh();
        else if (screen instanceof UeDetailsScreen us) us.refresh();
        else if (screen instanceof EvaluationsScreen es) es.refresh();
        else if (screen instanceof AnalyticsScreen as) as.refresh();
        else if (screen instanceof PlanningScreen ps) ps.refresh();
        else if (screen instanceof EngineConfigScreen ecs) ecs.refresh();

        centerContainer.getChildren().setAll(screen);

        FadeTransition ft = new FadeTransition(Duration.millis(150), screen);
        ft.setFromValue(0.7);
        ft.setToValue(1.0);
        ft.play();
    }

    private void openNewEvaluationDialog() {
        SemesterSummary sem = controller.getContext().getActiveSemester();
        EvaluationDialog dlg = new EvaluationDialog(controller.getEngine(), sem != null ? sem.id() : null, null);
        dlg.showAndWait().ifPresent(draft -> controller.createEvaluation(draft, id -> {
            showScreen(controller.getContext().getCurrentDestination());
        }));
    }

    private void displayToast(String message, AppContext.ToastType type) {
        Label toast = new Label(message);
        toast.getStyleClass().add("card");
        toast.setStyle("-fx-padding: 10px 18px; -fx-font-weight: 600; -fx-background-radius: 8px;");

        switch (type) {
            case SUCCESS ->
                    toast.setStyle(toast.getStyle() + " -fx-border-color: -fx-success-solid; -fx-text-fill: -fx-success-text; -fx-background-color: -fx-success-bg;");
            case WARNING ->
                    toast.setStyle(toast.getStyle() + " -fx-border-color: -fx-warning-solid; -fx-text-fill: -fx-warning-text; -fx-background-color: -fx-warning-bg;");
            case ERROR ->
                    toast.setStyle(toast.getStyle() + " -fx-border-color: -fx-danger-solid; -fx-text-fill: -fx-danger-text; -fx-background-color: -fx-danger-bg;");
            case INFO ->
                    toast.setStyle(toast.getStyle() + " -fx-border-color: -fx-primary-600; -fx-text-fill: -fx-text-primary;");
        }

        toastContainer.getChildren().add(toast);

        FadeTransition fadeIn = new FadeTransition(Duration.millis(200), toast);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);
        fadeIn.play();

        PauseTransition delay = new PauseTransition(Duration.seconds(3.5));
        delay.setOnFinished(e -> {
            FadeTransition fadeOut = new FadeTransition(Duration.millis(200), toast);
            fadeOut.setFromValue(1);
            fadeOut.setToValue(0);
            fadeOut.setOnFinished(ev -> toastContainer.getChildren().remove(toast));
            fadeOut.play();
        });
        delay.play();
    }

    public void applyTheme(UserPreferences.ThemeMode mode) {
        getStylesheets().clear();

        String vars = mode == UserPreferences.ThemeMode.DARK
                ? getClass().getResource("/styles/variables-dark.css").toExternalForm()
                : getClass().getResource("/styles/variables-light.css").toExternalForm();

        getStylesheets().addAll(
                vars,
                getClass().getResource("/styles/base.css").toExternalForm(),
                getClass().getResource("/styles/charts.css").toExternalForm()
        );
    }
}
