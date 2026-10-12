package api.views.screens;

import api.controllers.AppController;
import api.views.dialogs.ConfirmationDialog;
import api.views.widgets.EmptyState;
import application.engine.GradeEngine;
import application.engine.config.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.feather.Feather;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Écran 9 — Paramétrage du moteur de calcul (EngineConfigScreen).
 * Génère dynamiquement les formulaires de configuration décrits par le moteur métier.
 */
public class EngineConfigScreen extends ScrollPane {

    private final AppController controller;
    private final VBox content = new VBox(20);

    private final Map<String, Control> inputControls = new LinkedHashMap<>();

    public EngineConfigScreen(AppController controller) {
        this.controller = controller;

        setFitToWidth(true);
        setStyle("-fx-background-color: transparent;");
        content.setPadding(new Insets(24));
        setContent(content);

        refresh();
    }

    public void refresh() {
        content.getChildren().clear();
        inputControls.clear();

        GradeEngine engine = controller.getEngine();

        if (!engine.capabilities().configuration()) {
            content.getChildren().add(new EmptyState(
                    Feather.SLIDERS,
                    "Configuration non disponible",
                    "Le moteur de calcul connecté ne supporte pas de configuration dynamique des formules.",
                    null, null
            ));
            return;
        }

        ConfigurationScope scope = ConfigurationScope.global();
        ConfigurationView configView;
        try {
            configView = engine.getConfiguration(scope);
        } catch (Exception e) {
            content.getChildren().add(new EmptyState(
                    Feather.ALERT_CIRCLE,
                    "Erreur de configuration",
                    e.getMessage(),
                    null, null
            ));
            return;
        }

        // En-tête
        VBox titleBox = new VBox(4);
        Label title = new Label(configView.title());
        title.getStyleClass().add("heading-xl");
        Label subtitle = new Label(configView.description());
        subtitle.getStyleClass().add("text-muted");
        titleBox.getChildren().addAll(title, subtitle);
        content.getChildren().add(titleBox);

        // Sections
        for (ConfigurationSection section : configView.sections()) {
            VBox secBox = new VBox(12);
            secBox.getStyleClass().add("card");

            VBox secTitleBox = new VBox(2);
            Label secTitle = new Label(section.title());
            secTitle.getStyleClass().add("heading-md");
            Label secDesc = new Label(section.description());
            secDesc.getStyleClass().add("text-muted");
            secDesc.setStyle("-fx-font-size: 11px;");
            secTitleBox.getChildren().addAll(secTitle, secDesc);
            secBox.getChildren().add(secTitleBox);

            GridPane grid = new GridPane();
            grid.setHgap(16);
            grid.setVgap(12);
            grid.setStyle("-fx-padding: 8px 0;");

            int row = 0;
            for (ConfigurationParameter param : section.parameters()) {
                VBox pLabelBox = new VBox(2);
                Label pLabel = new Label(param.label());
                pLabel.getStyleClass().add("heading-sm");
                Label pDesc = new Label(param.description());
                pDesc.getStyleClass().add("text-tertiary");
                pDesc.setStyle("-fx-font-size: 11px;");
                pDesc.setWrapText(true);
                pDesc.setMaxWidth(350);
                pLabelBox.getChildren().addAll(pLabel, pDesc);

                Control editor = createEditor(param);
                inputControls.put(param.key(), editor);

                grid.add(pLabelBox, 0, row);
                grid.add(editor, 1, row);
                row++;
            }

            secBox.getChildren().add(grid);
            content.getChildren().add(secBox);
        }

        // Barre d'actions
        HBox actions = new HBox(12);
        actions.setAlignment(Pos.CENTER_RIGHT);

        Button resetBtn = new Button("Rétablir les valeurs par défaut");
        resetBtn.getStyleClass().add("btn-secondary");
        resetBtn.setOnAction(e -> {
            if (ConfirmationDialog.confirm("Réinitialiser les paramètres",
                    "Rétablir la configuration par défaut ?",
                    "Toutes les modifications personnalisées de calcul seront effacées.")) {
                engine.resetConfiguration(scope);
                controller.getContext().showToast("Configuration réinitialisée.", api.model.AppContext.ToastType.INFO);
                refresh();
            }
        });

        Button saveBtn = new Button("Enregistrer les paramètres");
        saveBtn.getStyleClass().add("btn-primary");
        saveBtn.setOnAction(e -> {
            Map<String, ConfigValue> changes = new HashMap<>();
            for (Map.Entry<String, Control> entry : inputControls.entrySet()) {
                String key = entry.getKey();
                Control ctrl = entry.getValue();
                if (ctrl instanceof TextField tf) {
                    try {
                        changes.put(key, ConfigValue.number(Double.parseDouble(tf.getText().trim())));
                    } catch (NumberFormatException ignored) {
                    }
                } else if (ctrl instanceof CheckBox cb) {
                    changes.put(key, ConfigValue.bool(cb.isSelected()));
                } else if (ctrl instanceof ComboBox<?> combo) {
                    Object val = combo.getValue();
                    if (val instanceof Choice c) {
                        changes.put(key, ConfigValue.choice(c.code()));
                    }
                }
            }
            controller.updateConfiguration(scope, changes, this::refresh);
        });

        actions.getChildren().addAll(resetBtn, saveBtn);
        content.getChildren().add(actions);
    }

    private Control createEditor(ConfigurationParameter param) {
        if (param.value() instanceof ConfigValue.NumberValue nv) {
            TextField tf = new TextField(String.valueOf(nv.value()));
            tf.setPrefWidth(120);
            return tf;
        } else if (param.value() instanceof ConfigValue.BooleanValue bv) {
            CheckBox cb = new CheckBox();
            cb.setSelected(bv.value());
            return cb;
        } else if (param.value() instanceof ConfigValue.ChoiceValue cv) {
            ComboBox<Choice> combo = new ComboBox<>();
            combo.getItems().setAll(param.constraints().choices());
            for (Choice c : param.constraints().choices()) {
                if (c.code().equals(cv.code())) {
                    combo.setValue(c);
                    break;
                }
            }
            combo.setConverter(new javafx.util.StringConverter<>() {
                @Override
                public String toString(Choice object) {
                    return object != null ? object.label() : "";
                }

                @Override
                public Choice fromString(String string) {
                    return null;
                }
            });
            return combo;
        } else {
            return new TextField();
        }
    }
}
