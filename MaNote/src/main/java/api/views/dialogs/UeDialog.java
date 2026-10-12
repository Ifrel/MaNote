package api.views.dialogs;

import application.engine.model.SemesterId;
import application.engine.model.SemesterSummary;
import application.engine.model.UeDraft;
import application.engine.model.UeSummary;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.util.StringConverter;

import java.util.List;

/**
 * Dialogue pour créer ou modifier une Unité d'Enseignement (UE).
 */
public class UeDialog extends Dialog<UeDraft> {

    private final ComboBox<SemesterSummary> semesterCombo = new ComboBox<>();
    private final TextField codeField = new TextField();
    private final TextField nameField = new TextField();
    private final Spinner<Integer> creditsSpinner = new Spinner<>(1, 60, 6);
    private final TextArea descriptionArea = new TextArea();
    private final Label errorLabel = new Label();

    public UeDialog(List<SemesterSummary> semesters, SemesterId currentSemesterId, UeSummary toEdit) {
        setTitle(toEdit == null ? "Nouvelle Unité d'Enseignement (UE)" : "Modifier l'UE");
        setHeaderText(toEdit == null ? "Ajoutez une UE à votre semestre" : "Mettez à jour les informations de l'UE");

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

        // Sélection du semestre
        semesterCombo.getItems().setAll(semesters);
        semesterCombo.setConverter(new StringConverter<>() {
            @Override
            public String toString(SemesterSummary s) {
                return s != null ? s.label() : "";
            }

            @Override
            public SemesterSummary fromString(String string) {
                return null;
            }
        });
        if (currentSemesterId != null) {
            for (SemesterSummary s : semesters) {
                if (s.id().equals(currentSemesterId)) {
                    semesterCombo.setValue(s);
                    break;
                }
            }
        } else if (!semesters.isEmpty()) {
            semesterCombo.setValue(semesters.get(0));
        }

        grid.add(new Label("Semestre :"), 0, 0);
        grid.add(semesterCombo, 1, 0);

        // Code UE
        codeField.setPromptText("Ex: INFO-M1-CL");
        grid.add(new Label("Code UE :"), 0, 1);
        grid.add(codeField, 1, 1);

        // Intitulé
        nameField.setPromptText("Ex: Conception Logicielle Avancée");
        grid.add(new Label("Intitulé / Nom :"), 0, 2);
        grid.add(nameField, 1, 2);

        // Crédits ECTS
        creditsSpinner.setEditable(true);
        grid.add(new Label("Crédits ECTS :"), 0, 3);
        grid.add(creditsSpinner, 1, 3);

        // Description
        descriptionArea.setPromptText("Description facultative des objectifs ou compétences");
        descriptionArea.setPrefRowCount(2);
        grid.add(new Label("Description :"), 0, 4);
        grid.add(descriptionArea, 1, 4);

        errorLabel.getStyleClass().add("field-error");
        errorLabel.setVisible(false);
        grid.add(errorLabel, 1, 5);

        if (toEdit != null) {
            codeField.setText(toEdit.code());
            nameField.setText(toEdit.name());
            creditsSpinner.getValueFactory().setValue(toEdit.credits());
        }

        pane.setContent(grid);

        saveBtn.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            if (semesterCombo.getValue() == null) {
                errorLabel.setText("Veuillez sélectionner un semestre.");
                errorLabel.setVisible(true);
                event.consume();
                return;
            }
            if (codeField.getText().isBlank()) {
                errorLabel.setText("Le code de l'UE est obligatoire.");
                errorLabel.setVisible(true);
                event.consume();
                return;
            }
            if (nameField.getText().isBlank()) {
                errorLabel.setText("L'intitulé de l'UE est obligatoire.");
                errorLabel.setVisible(true);
                event.consume();
            }
        });

        setResultConverter(dialogButton -> {
            if (dialogButton == saveBtnType) {
                return new UeDraft(
                        semesterCombo.getValue().id(),
                        codeField.getText().trim(),
                        nameField.getText().trim(),
                        creditsSpinner.getValue(),
                        descriptionArea.getText().trim()
                );
            }
            return null;
        });
    }
}
