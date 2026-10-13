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

import java.util.Locale;
import java.util.function.Consumer;

/**
 * Carte d'une Unité d'Enseignement : la moyenne est l'élément dominant, teintée selon la validation.
 */
public class UECard extends VBox {

    public UECard(UeSummary ue, Consumer<UeSummary> onClick) {
        getStyleClass().add("card-interactive");
        setSpacing(12);

        // Code, crédits, statut
        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);
        Label codeLabel = new Label(ue.code());
        codeLabel.getStyleClass().add("heading-sm");
        Label creditBadge = new Label(ue.credits() + " ECTS");
        creditBadge.getStyleClass().addAll("badge", "badge-neutral");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        header.getChildren().addAll(codeLabel, creditBadge, spacer, StatusBadge.ofValidation(ue.validation()));

        // Intitulé (deux lignes réservées pour aligner les cartes d'une même rangée)
        Label nameLabel = new Label(ue.name());
        nameLabel.getStyleClass().addAll("heading-md");
        nameLabel.setWrapText(true);
        nameLabel.setMinHeight(40);
        nameLabel.setMaxHeight(40);
        nameLabel.setAlignment(Pos.TOP_LEFT);

        // Moyenne
        HBox scoreBox = new HBox(8);
        scoreBox.setAlignment(Pos.BASELINE_LEFT);
        Label scoreValue = new Label();
        scoreValue.setStyle("-fx-font-size: 28px; -fx-font-weight: bold;");
        if (ue.average() instanceof ComputedResult.Value val) {
            scoreValue.setText(String.format(Locale.FRENCH, "%.2f", val.value()));
            scoreValue.getStyleClass().add(ScoreRing.tone(ue.validation().state()));
            Label outOf = new Label("/ " + String.format(Locale.FRENCH, "%.0f", val.outOf())
                    + (val.nature() == application.engine.model.ResultNature.FINAL ? "" : " · " + natureLabel(val)));
            outOf.getStyleClass().add("text-muted");
            scoreBox.getChildren().addAll(scoreValue, outOf);
        } else {
            scoreValue.setText("—");
            scoreValue.getStyleClass().add("text-tertiary");
            Label pending = new Label("Pas encore de note");
            pending.getStyleClass().add("text-muted");
            scoreBox.getChildren().addAll(scoreValue, pending);
        }

        // Progression
        EvaluationProgress progress = ue.progress();
        HBox progressInfo = new HBox(8);
        progressInfo.setAlignment(Pos.CENTER_LEFT);
        Label progressLabel = new Label(String.format("%d/%d évaluée(s)", progress.graded(), progress.total()));
        progressLabel.getStyleClass().add("text-tertiary");
        progressLabel.setStyle("-fx-font-size: 11px;");
        Region pSpacer = new Region();
        HBox.setHgrow(pSpacer, Priority.ALWAYS);
        progressInfo.getChildren().addAll(progressLabel, pSpacer);
        ue.attention().ifPresent(text -> {
            Label warn = new Label(text);
            warn.getStyleClass().addAll("badge", "badge-warning");
            warn.setStyle("-fx-font-size: 10px;");
            progressInfo.getChildren().add(warn);
        });

        ProgressBar bar = new ProgressBar(progress.gradedRatio());
        bar.setMaxWidth(Double.MAX_VALUE);
        bar.setMinHeight(6);
        bar.setPrefHeight(6);

        getChildren().addAll(header, nameLabel, scoreBox, new VBox(5, progressInfo, bar));

        if (onClick != null) {
            setOnMouseClicked(e -> onClick.accept(ue));
        }
    }

    private static String natureLabel(ComputedResult.Value v) {
        return v.nature() == application.engine.model.ResultNature.PROVISIONAL ? "provisoire" : "simulé";
    }
}
