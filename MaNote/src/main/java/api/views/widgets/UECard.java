package api.views.widgets;

import application.engine.model.ComputedResult;
import application.engine.model.EvaluationProgress;
import application.engine.model.UeSummary;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.function.Consumer;

/**
 * Carte visuelle représentant une Unité d'Enseignement (UE).
 */
public class UECard extends VBox {

    public UECard(UeSummary ue, Consumer<UeSummary> onClick) {
        getStyleClass().addAll("card-interactive");
        setSpacing(10);

        // Header: Code UE, Crédits ECTS, et Badge Validation
        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);

        Label codeLabel = new Label(ue.code());
        codeLabel.getStyleClass().add("heading-sm");

        Label creditBadge = new Label(ue.credits() + " ECTS");
        creditBadge.getStyleClass().addAll("badge", "badge-neutral");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        StatusBadge statusBadge = StatusBadge.ofValidation(ue.validation());

        header.getChildren().addAll(codeLabel, creditBadge, spacer, statusBadge);

        // Intitulé de l'UE
        Label nameLabel = new Label(ue.name());
        nameLabel.getStyleClass().add("heading-md");
        nameLabel.setWrapText(true);

        // Moyenne et Progression
        HBox scoreBox = new HBox(8);
        scoreBox.setAlignment(Pos.BASELINE_LEFT);

        Label scoreValue = new Label();
        scoreValue.getStyleClass().add("heading-lg");

        if (ue.average() instanceof ComputedResult.Value val) {
            scoreValue.setText(String.format(java.util.Locale.FRENCH, "%.2f", val.value()));
            Label outOf = new Label("/ " + String.format(java.util.Locale.FRENCH, "%.0f", val.outOf()));
            outOf.getStyleClass().add("text-muted");
            scoreBox.getChildren().addAll(scoreValue, outOf);
        } else {
            scoreValue.setText("—");
            Label pendingText = new Label("Pas encore de note");
            pendingText.getStyleClass().add("text-muted");
            scoreBox.getChildren().addAll(scoreValue, pendingText);
        }

        // Barre de progression des évaluations
        EvaluationProgress progress = ue.progress();
        VBox progressBox = new VBox(4);
        HBox progressInfo = new HBox(8);
        progressInfo.setAlignment(Pos.CENTER_LEFT);

        Label progressLabel = new Label(String.format("%d/%d évaluée(s)", progress.graded(), progress.total()));
        progressLabel.getStyleClass().add("text-tertiary");
        progressLabel.setStyle("-fx-font-size: 11px;");

        Region pSpacer = new Region();
        HBox.setHgrow(pSpacer, Priority.ALWAYS);

        if (ue.attention().isPresent()) {
            Label warn = new Label(ue.attention().get());
            warn.getStyleClass().addAll("badge", "badge-warning");
            warn.setStyle("-fx-font-size: 10px;");
            progressInfo.getChildren().addAll(progressLabel, pSpacer, warn);
        } else {
            progressInfo.getChildren().addAll(progressLabel, pSpacer);
        }

        ProgressBar progressBar = new ProgressBar(progress.gradedRatio());
        progressBar.setMaxWidth(Double.MAX_VALUE);
        progressBar.setStyle("-fx-accent: -fx-primary-600; -fx-pref-height: 6px;");

        progressBox.getChildren().addAll(progressInfo, progressBar);

        getChildren().addAll(header, nameLabel, scoreBox, progressBox);

        if (onClick != null) {
            setOnMouseClicked(e -> onClick.accept(ue));
        }
    }
}
