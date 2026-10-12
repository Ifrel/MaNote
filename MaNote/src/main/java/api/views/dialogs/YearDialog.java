package api.views.dialogs;

import application.engine.model.AcademicYearDraft;
import application.engine.model.AcademicYearSummary;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;

import java.time.LocalDate;

/**
 * Dialogue de création ou de modification d'une année universitaire.
 */
public class YearDialog extends Dialog<AcademicYearDraft> {

    private final TextField labelField = new TextField();
    private final TextField programmeField = new TextField();
    private final Label errorLabel = new Label();

    public YearDialog(AcademicYearSummary toEdit) {
        setTitle(toEdit == null ? "Nouvelle Année Universitaire" : "Modifier l'Année Universitaire");
        setHeaderText(toEdit == null ? "Définissez une nouvelle année universitaire (ex: Master 1 2025-2026)" : "Mettez à jour le libellé de l'année");

        DialogPane pane = getDialogPane();
        pane.getStyleClass().add("card");
        pane.getStylesheets().addAll(
                getClass().getResource("/styles/variables-light.css").toExternalForm(),
                getClass().getResource("/styles/base.css").toExternalForm()
        );

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

        labelField.setPromptText("Ex: Master 1 Informatique (2025-2026)");
        grid.add(new Label("Intitulé de l'année :"), 0, 0);
        grid.add(labelField, 1, 0);

        programmeField.setPromptText("Ex: Informatique");
        grid.add(new Label("Cursus / Filière :"), 0, 1);
        grid.add(programmeField, 1, 1);

        errorLabel.getStyleClass().add("field-error");
        errorLabel.setVisible(false);
        grid.add(errorLabel, 1, 2);

        if (toEdit != null) {
            labelField.setText(toEdit.label());
            programmeField.setText(toEdit.programme());
        }

        pane.setContent(grid);

        saveBtn.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            if (labelField.getText().isBlank()) {
                errorLabel.setText("L'intitulé de l'année est obligatoire.");
                errorLabel.setVisible(true);
                event.consume();
            }
        });

        setResultConverter(dialogButton -> {
            if (dialogButton == saveBtnType) {
                String prog = programmeField.getText().isBlank() ? labelField.getText().trim() : programmeField.getText().trim();
                return new AcademicYearDraft(
                        labelField.getText().trim(),
                        prog,
                        LocalDate.now(),
                        LocalDate.now().plusYears(1)
                );
            }
            return null;
        });
    }
}
