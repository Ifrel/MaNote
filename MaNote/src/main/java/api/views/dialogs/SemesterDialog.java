package api.views.dialogs;

import api.views.app.Theme;
import application.engine.model.AcademicYearId;
import application.engine.model.AcademicYearSummary;
import application.engine.model.SemesterDraft;
import application.engine.model.SemesterSummary;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.util.StringConverter;

import java.time.LocalDate;
import java.util.List;

/**
 * Dialogue de création ou de modification d'un semestre.
 */
public class SemesterDialog extends Dialog<SemesterDraft> {

    private final ComboBox<AcademicYearSummary> yearCombo = new ComboBox<>();
    private final Spinner<Integer> numberSpinner = new Spinner<>(1, 12, 1);
    private final TextField labelField = new TextField();
    private final Label errorLabel = new Label();

    public SemesterDialog(List<AcademicYearSummary> years, AcademicYearId currentYearId, SemesterSummary toEdit) {
        setTitle(toEdit == null ? "Nouveau Semestre" : "Modifier le Semestre");
        setHeaderText(toEdit == null ? "Ajoutez un semestre à votre cursus (ex: Semestre 7)" : "Mettez à jour le semestre");

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

        // Sélection de l'année
        yearCombo.getItems().setAll(years);
        yearCombo.setConverter(new StringConverter<>() {
            @Override
            public String toString(AcademicYearSummary y) {
                return y != null ? y.label() : "";
            }

            @Override
            public AcademicYearSummary fromString(String string) {
                return null;
            }
        });
        if (currentYearId != null) {
            for (AcademicYearSummary y : years) {
                if (y.id().equals(currentYearId)) {
                    yearCombo.setValue(y);
                    break;
                }
            }
        } else if (!years.isEmpty()) {
            yearCombo.setValue(years.get(0));
        }

        grid.add(new Label("Année universitaire :"), 0, 0);
        grid.add(yearCombo, 1, 0);

        // Numéro de semestre
        numberSpinner.setEditable(true);
        grid.add(new Label("Numéro du semestre :"), 0, 1);
        grid.add(numberSpinner, 1, 1);

        // Intitulé
        labelField.setPromptText("Ex: Semestre 7 (M1 S1)");
        grid.add(new Label("Libellé :"), 0, 2);
        grid.add(labelField, 1, 2);

        // Mise à jour automatique du libellé selon le numéro si non saisi
        numberSpinner.valueProperty().addListener((obs, oldV, newV) -> {
            if (labelField.getText().isBlank() || labelField.getText().startsWith("Semestre ")) {
                labelField.setText("Semestre " + newV);
            }
        });

        errorLabel.getStyleClass().add("field-error");
        errorLabel.setVisible(false);
        grid.add(errorLabel, 1, 3);

        if (toEdit != null) {
            numberSpinner.getValueFactory().setValue(toEdit.number());
            labelField.setText(toEdit.label());
        } else {
            labelField.setText("Semestre 1");
        }

        pane.setContent(grid);

        saveBtn.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            if (yearCombo.getValue() == null) {
                errorLabel.setText("Veuillez sélectionner une année universitaire.");
                errorLabel.setVisible(true);
                event.consume();
                return;
            }
            if (labelField.getText().isBlank()) {
                errorLabel.setText("Le libellé du semestre est obligatoire.");
                errorLabel.setVisible(true);
                event.consume();
            }
        });

        setResultConverter(dialogButton -> {
            if (dialogButton == saveBtnType) {
                LocalDate start = LocalDate.now();
                LocalDate end = start.plusMonths(6);
                return new SemesterDraft(
                        yearCombo.getValue().id(),
                        numberSpinner.getValue(),
                        labelField.getText().trim(),
                        start,
                        end
                );
            }
            return null;
        });
    }
}
