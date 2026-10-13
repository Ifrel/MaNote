package api.views.dialogs;

import api.views.app.Theme;
import application.engine.GradeEngine;
import application.engine.model.*;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.util.StringConverter;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;

/**
 * Dialogue moderne pour la création ou la modification d'une évaluation.
 */
public class EvaluationDialog extends Dialog<EvaluationDraft> {

    private final TextField titleField = new TextField();
    private final ComboBox<UeSummary> ueCombo = new ComboBox<>();
    private final ComboBox<EvaluationType> typeCombo = new ComboBox<>();
    private final DatePicker datePicker = new DatePicker();
    private final ComboBox<EvaluationStatus> statusCombo = new ComboBox<>();
    private final TextField maxPointsField = new TextField("20");
    private final TextField weightField = new TextField("1.0");
    private final TextArea descriptionArea = new TextArea();
    private final Label errorLabel = new Label();

    public EvaluationDialog(GradeEngine engine, SemesterId currentSemesterId, EvaluationSummary toEdit) {
        setTitle(toEdit == null ? "Nouvelle évaluation" : "Modifier l'évaluation");
        setHeaderText(toEdit == null ? "Planifier ou ajouter une nouvelle évaluation" : "Mettre à jour les informations de l'évaluation");

        DialogPane pane = getDialogPane();
        pane.getStyleClass().add("card");
        Theme.styleDialog(pane);

        ButtonType saveBtnType = new ButtonType("Enregistrer", ButtonBar.ButtonData.OK_DONE);
        pane.getButtonTypes().addAll(saveBtnType, ButtonType.CANCEL);

        Button saveBtn = (Button) pane.lookupButton(saveBtnType);
        saveBtn.getStyleClass().add("btn-primary");
        Button cancelBtn = (Button) pane.lookupButton(ButtonType.CANCEL);
        cancelBtn.getStyleClass().add("btn-secondary");

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(12);
        grid.setPadding(new Insets(16));

        // Intitulé
        titleField.setPromptText("Ex: Contrôle continu 1");
        grid.add(new Label("Titre :"), 0, 0);
        grid.add(titleField, 1, 0, 2, 1);

        // UE
        List<UeSummary> ues = currentSemesterId != null ? engine.getUes(currentSemesterId) : List.of();
        ueCombo.getItems().setAll(ues);
        ueCombo.setConverter(new StringConverter<>() {
            @Override
            public String toString(UeSummary ue) {
                return ue != null ? ue.code() + " — " + ue.name() : "";
            }

            @Override
            public UeSummary fromString(String string) {
                return null;
            }
        });
        if (!ues.isEmpty()) ueCombo.setValue(ues.get(0));
        grid.add(new Label("UE :"), 0, 1);
        grid.add(ueCombo, 1, 1, 2, 1);

        // Type
        List<EvaluationType> types = engine.getEvaluationTypes();
        typeCombo.getItems().setAll(types);
        typeCombo.setConverter(new StringConverter<>() {
            @Override
            public String toString(EvaluationType t) {
                return t != null ? t.label() : "";
            }

            @Override
            public EvaluationType fromString(String string) {
                return null;
            }
        });
        if (!types.isEmpty()) typeCombo.setValue(types.get(0));
        grid.add(new Label("Type :"), 0, 2);
        grid.add(typeCombo, 1, 2);

        // Date
        datePicker.setValue(LocalDate.now());
        grid.add(new Label("Date prévue :"), 0, 3);
        grid.add(datePicker, 1, 3);

        // Statut
        if (toEdit == null) {
            statusCombo.getItems().setAll(EvaluationStatus.PLANNED, EvaluationStatus.AWAITING_GRADE, EvaluationStatus.CANCELLED);
            statusCombo.setValue(EvaluationStatus.PLANNED);
        } else {
            statusCombo.getItems().setAll(EvaluationStatus.values());
            statusCombo.setValue(toEdit.status());
        }
        grid.add(new Label("Statut :"), 0, 4);
        grid.add(statusCombo, 1, 4);

        // Barème & Coefficient
        grid.add(new Label("Barème (max) :"), 0, 5);
        grid.add(maxPointsField, 1, 5);

        grid.add(new Label("Coefficient :"), 0, 6);
        grid.add(weightField, 1, 6);

        // Description
        descriptionArea.setPromptText("Informations complémentaires (salle, consignes...)");
        descriptionArea.setPrefRowCount(3);
        grid.add(new Label("Description :"), 0, 7);
        grid.add(descriptionArea, 1, 7, 2, 1);

        // Erreur
        errorLabel.getStyleClass().add("field-error");
        errorLabel.setVisible(false);
        grid.add(errorLabel, 1, 8, 2, 1);

        // Pré-remplissage en mode édition
        if (toEdit != null) {
            titleField.setText(toEdit.title());
            for (UeSummary u : ues) {
                if (u.id().equals(toEdit.ue().id())) {
                    ueCombo.setValue(u);
                    break;
                }
            }
            typeCombo.setValue(toEdit.type());
            toEdit.date().ifPresent(datePicker::setValue);
            maxPointsField.setText(String.valueOf(toEdit.maxPoints()));
            toEdit.weight().ifPresent(w -> weightField.setText(String.valueOf(w)));
        }

        pane.setContent(grid);

        // Validation avant fermeture
        saveBtn.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            if (titleField.getText().isBlank()) {
                showError("Le titre de l'évaluation est obligatoire.");
                event.consume();
                return;
            }
            if (ueCombo.getValue() == null) {
                showError("Veuillez sélectionner une UE.");
                event.consume();
                return;
            }
            try {
                double max = Double.parseDouble(maxPointsField.getText().trim());
                if (max <= 0) {
                    showError("Le barème doit être strictement positif.");
                    event.consume();
                    return;
                }
            } catch (NumberFormatException e) {
                showError("Le barème doit être un nombre valide.");
                event.consume();
                return;
            }
            if (!weightField.getText().isBlank()) {
                try {
                    double w = Double.parseDouble(weightField.getText().trim());
                    if (w < 0) {
                        showError("Le coefficient doit être positif ou nul.");
                        event.consume();
                        return;
                    }
                } catch (NumberFormatException e) {
                    showError("Le coefficient doit être un nombre valide.");
                    event.consume();
                    return;
                }
            }
        });

        setResultConverter(dialogButton -> {
            if (dialogButton == saveBtnType) {
                double max = Double.parseDouble(maxPointsField.getText().trim());
                OptionalDouble w = weightField.getText().isBlank()
                        ? OptionalDouble.empty()
                        : OptionalDouble.of(Double.parseDouble(weightField.getText().trim()));
                Optional<LocalDate> d = Optional.ofNullable(datePicker.getValue());

                return new EvaluationDraft(
                        titleField.getText().trim(),
                        ueCombo.getValue().id(),
                        typeCombo.getValue() != null ? typeCombo.getValue().code() : "OTHER",
                        d,
                        statusCombo.getValue(),
                        max,
                        w,
                        descriptionArea.getText().trim()
                );
            }
            return null;
        });
    }

    private void showError(String msg) {
        errorLabel.setText(msg);
        errorLabel.setVisible(true);
    }
}
