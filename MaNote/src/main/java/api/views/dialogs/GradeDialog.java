package api.views.dialogs;

import application.engine.model.EvaluationSummary;
import application.engine.model.GradeEntry;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;

/**
 * Dialogue pour la saisie ou la modification d'une note.
 */
public class GradeDialog extends Dialog<GradeDialog.GradeDialogResult> {

    public sealed interface GradeDialogResult {
        record Save(GradeEntry entry) implements GradeDialogResult {
        }

        record Clear() implements GradeDialogResult {
        }
    }

    private final RadioButton scoreRadio = new RadioButton("Note chiffrée");
    private final RadioButton absenceJustifiedRadio = new RadioButton("Absence justifiée (ABJ)");
    private final RadioButton absenceUnjustifiedRadio = new RadioButton("Absence injustifiée (ABI)");
    private final TextField pointsField = new TextField();
    private final Label maxLabel = new Label();
    private final Label errorLabel = new Label();

    public GradeDialog(EvaluationSummary eval) {
        setTitle("Saisir / Modifier la note");
        setHeaderText("Évaluation : " + eval.title() + " (" + eval.ue().code() + ")");

        DialogPane pane = getDialogPane();
        pane.getStyleClass().add("card");
        pane.getStylesheets().addAll(
                getClass().getResource("/styles/variables-light.css").toExternalForm(),
                getClass().getResource("/styles/base.css").toExternalForm()
        );

        ButtonType saveBtnType = new ButtonType("Valider la note", ButtonBar.ButtonData.OK_DONE);
        ButtonType clearBtnType = new ButtonType("Supprimer la note", ButtonBar.ButtonData.OTHER);
        pane.getButtonTypes().addAll(saveBtnType, ButtonType.CANCEL);

        if (eval.grade().isPresent()) {
            pane.getButtonTypes().add(0, clearBtnType);
        }

        Button saveBtn = (Button) pane.lookupButton(saveBtnType);
        saveBtn.getStyleClass().add("btn-primary");
        Button cancelBtn = (Button) pane.lookupButton(ButtonType.CANCEL);
        cancelBtn.getStyleClass().add("btn-secondary");

        ToggleGroup group = new ToggleGroup();
        scoreRadio.setToggleGroup(group);
        absenceJustifiedRadio.setToggleGroup(group);
        absenceUnjustifiedRadio.setToggleGroup(group);
        scoreRadio.setSelected(true);

        pointsField.setPromptText("Ex: 15.5");
        maxLabel.setText(" / " + String.format(java.util.Locale.FRENCH, "%.0f points", eval.maxPoints()));
        maxLabel.getStyleClass().add("text-muted");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(12);
        grid.setPadding(new Insets(16));

        grid.add(scoreRadio, 0, 0, 2, 1);
        grid.add(new Label("Points obtenus :"), 0, 1);
        grid.add(pointsField, 1, 1);
        grid.add(maxLabel, 2, 1);

        grid.add(new Separator(), 0, 2, 3, 1);
        grid.add(new Label("Ou déclarer une absence :"), 0, 3, 3, 1);
        grid.add(absenceJustifiedRadio, 0, 4, 3, 1);
        grid.add(absenceUnjustifiedRadio, 0, 5, 3, 1);

        errorLabel.getStyleClass().add("field-error");
        errorLabel.setVisible(false);
        grid.add(errorLabel, 0, 6, 3, 1);

        // Désactivation du champ points si absence
        group.selectedToggleProperty().addListener((obs, oldV, newV) -> {
            pointsField.setDisable(newV != scoreRadio);
        });

        // Pré-remplissage si une note existe
        if (eval.grade().isPresent()) {
            GradeEntry g = eval.grade().get();
            if (g instanceof GradeEntry.Score score) {
                scoreRadio.setSelected(true);
                pointsField.setText(String.valueOf(score.points()));
            } else if (g instanceof GradeEntry.Absence ab) {
                if (ab.justified()) {
                    absenceJustifiedRadio.setSelected(true);
                } else {
                    absenceUnjustifiedRadio.setSelected(true);
                }
            }
        }

        pane.setContent(grid);

        // Validation
        saveBtn.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            if (scoreRadio.isSelected()) {
                if (pointsField.getText().isBlank()) {
                    showError("Veuillez saisir une note ou cocher une absence.");
                    event.consume();
                    return;
                }
                try {
                    double pts = Double.parseDouble(pointsField.getText().trim());
                    if (pts < 0) {
                        showError("La note doit être positive ou nulle.");
                        event.consume();
                        return;
                    }
                    if (pts > eval.maxPoints()) {
                        showError(String.format("La note (%.2f) ne peut pas dépasser le barème (%.2f).", pts, eval.maxPoints()));
                        event.consume();
                        return;
                    }
                } catch (NumberFormatException e) {
                    showError("Format de note invalide.");
                    event.consume();
                    return;
                }
            }
        });

        setResultConverter(btnType -> {
            if (btnType == saveBtnType) {
                if (scoreRadio.isSelected()) {
                    double pts = Double.parseDouble(pointsField.getText().trim());
                    return new GradeDialogResult.Save(GradeEntry.score(pts, eval.maxPoints()));
                } else if (absenceJustifiedRadio.isSelected()) {
                    return new GradeDialogResult.Save(GradeEntry.absence(true));
                } else if (absenceUnjustifiedRadio.isSelected()) {
                    return new GradeDialogResult.Save(GradeEntry.absence(false));
                }
            } else if (btnType == clearBtnType) {
                return new GradeDialogResult.Clear();
            }
            return null;
        });
    }

    private void showError(String msg) {
        errorLabel.setText(msg);
        errorLabel.setVisible(true);
    }
}
