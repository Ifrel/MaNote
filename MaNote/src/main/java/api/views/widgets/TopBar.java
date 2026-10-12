package api.views.widgets;

import api.controllers.AppController;
import api.model.AppContext;
import application.engine.model.SemesterSummary;
import infrastructure.preferences.UserPreferences;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.util.StringConverter;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;

/**
 * Barre supérieure de l'application (TopBar).
 */
public class TopBar extends HBox {

    private final AppController controller;
    private final Label titleLabel = new Label();
    private final ComboBox<SemesterSummary> semesterCombo = new ComboBox<>();
    private final Button themeToggleBtn = new Button();
    private final FontIcon themeIcon = new FontIcon(Feather.MOON);

    public TopBar(AppController controller, Runnable onNewEvaluation) {
        this.controller = controller;
        AppContext context = controller.getContext();

        getStyleClass().add("topbar");
        setAlignment(Pos.CENTER_LEFT);
        setSpacing(16);

        // Titre lié à la destination courante
        titleLabel.getStyleClass().add("heading-lg");
        titleLabel.textProperty().bind(context.currentDestinationProperty().map(dest -> dest != null ? dest.getTitle() : ""));

        getChildren().add(titleLabel);
        if (controller.getEngine().info().demonstration()) {
            Label demoBadge = new Label("Mode Démonstration");
            demoBadge.getStyleClass().addAll("badge", "badge-warning");
            getChildren().add(demoBadge);
        }

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        getChildren().add(spacer);

        // Sélecteur de Semestre
        semesterCombo.getStyleClass().add("combo-box");
        semesterCombo.setPrefWidth(180);
        semesterCombo.setConverter(new StringConverter<>() {
            @Override
            public String toString(SemesterSummary s) {
                return s != null ? s.label() : "—";
            }

            @Override
            public SemesterSummary fromString(String string) {
                return null;
            }
        });

        semesterCombo.setOnAction(e -> {
            SemesterSummary sel = semesterCombo.getValue();
            if (sel != null && (context.getActiveSemester() == null || !sel.id().equals(context.getActiveSemester().id()))) {
                context.setActiveSemester(sel);
            }
        });

        context.activeSemesterProperty().addListener((obs, oldV, newV) -> {
            if (newV != null) {
                for (SemesterSummary s : semesterCombo.getItems()) {
                    if (s.id().equals(newV.id())) {
                        if (semesterCombo.getValue() != s) {
                            semesterCombo.setValue(s);
                        }
                        break;
                    }
                }
            }
        });

        // Bouton Ajouter une évaluation rapide
        Button addEvalBtn = new Button("Nouvelle évaluation");
        addEvalBtn.getStyleClass().add("btn-primary");
        FontIcon plusIcon = new FontIcon(Feather.PLUS);
        plusIcon.setIconSize(14);
        addEvalBtn.setGraphic(plusIcon);
        if (onNewEvaluation != null) {
            addEvalBtn.setOnAction(e -> onNewEvaluation.run());
        }

        // Bouton Thème clair / sombre
        themeToggleBtn.getStyleClass().add("btn-icon");
        themeIcon.setIconSize(16);
        themeToggleBtn.setGraphic(themeIcon);
        themeToggleBtn.setOnAction(e -> controller.toggleTheme());

        updateThemeIcon(context.getThemeMode());
        context.themeModeProperty().addListener((obs, oldV, newV) -> updateThemeIcon(newV));

        getChildren().addAll(semesterCombo, addEvalBtn, themeToggleBtn);
    }

    public void updateSemesters(java.util.List<SemesterSummary> semesters) {
        semesterCombo.getItems().setAll(semesters);
        if (semesters.isEmpty()) {
            semesterCombo.setValue(null);
            semesterCombo.setPromptText("Aucun semestre");
            controller.getContext().setActiveSemester(null);
            return;
        }
        SemesterSummary active = controller.getContext().getActiveSemester();
        if (active != null) {
            for (SemesterSummary s : semesters) {
                if (s.id().equals(active.id())) {
                    semesterCombo.setValue(s);
                    return;
                }
            }
        }
        semesterCombo.setValue(semesters.get(0));
        controller.getContext().setActiveSemester(semesters.get(0));
    }

    private void updateThemeIcon(UserPreferences.ThemeMode mode) {
        if (mode == UserPreferences.ThemeMode.DARK) {
            themeIcon.setIconCode(Feather.SUN);
        } else {
            themeIcon.setIconCode(Feather.MOON);
        }
    }
}
