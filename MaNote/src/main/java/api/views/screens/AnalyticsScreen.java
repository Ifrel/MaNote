package api.views.screens;

import api.controllers.AppController;
import api.views.format.Formatters;
import api.views.widgets.EmptyState;
import application.engine.GradeEngine;
import application.engine.model.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.chart.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.feather.Feather;

import java.util.*;

/**
 * Écran 7 — Analyse & Progression (AnalyticsScreen).
 * Fournit des graphiques d'évolution, de comparaison d'UE, de distribution et un simulateur de notes.
 */
public class AnalyticsScreen extends ScrollPane {

    private final AppController controller;
    private final VBox content = new VBox(24);

    public AnalyticsScreen(AppController controller) {
        this.controller = controller;

        setFitToWidth(true);
        setStyle("-fx-background-color: transparent;");
        content.setPadding(new Insets(24));
        setContent(content);

        controller.getContext().activeSemesterProperty().addListener((obs, oldV, newV) -> refresh());
        refresh();
    }

    public void refresh() {
        content.getChildren().clear();
        SemesterSummary sem = controller.getContext().getActiveSemester();

        if (sem == null) {
            content.getChildren().add(new EmptyState(
                    Feather.BAR_CHART_2,
                    "Aucun semestre sélectionné",
                    "Sélectionnez un semestre pour visualiser les graphiques d'analyse.",
                    null, null
            ));
            return;
        }

        GradeEngine engine = controller.getEngine();

        // 1. Titre
        VBox titleBox = new VBox(4);
        Label title = new Label("Analyse & Progression des Résultats");
        title.getStyleClass().add("heading-xl");
        Label subtitle = new Label("Visualisez vos performances, comparez vos UE et simulez vos moyennes futures.");
        subtitle.getStyleClass().add("text-muted");
        titleBox.getChildren().addAll(title, subtitle);
        content.getChildren().add(titleBox);

        // 2. Grille de deux graphiques : BarChart (UEs) & LineChart (Timeline)
        HBox chartsGrid = new HBox(16);

        // BarChart : Moyennes par UE
        VBox barBox = new VBox(10);
        barBox.getStyleClass().add("card");
        HBox.setHgrow(barBox, Priority.ALWAYS);
        barBox.setPrefWidth(450);

        Label barTitle = new Label("Moyennes actuelles par UE (sur 20)");
        barTitle.getStyleClass().add("heading-md");

        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis(0, 20, 2);
        yAxis.setLabel("Note / 20");
        BarChart<String, Number> barChart = new BarChart<>(xAxis, yAxis);
        barChart.setLegendVisible(false);
        barChart.setAnimated(false);
        barChart.setPrefHeight(260);

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        List<UeSummary> ues = engine.getUes(sem.id());
        for (UeSummary ue : ues) {
            if (ue.average() instanceof ComputedResult.Value val) {
                series.getData().add(new XYChart.Data<>(ue.code(), val.value()));
            }
        }
        barChart.getData().add(series);
        barBox.getChildren().addAll(barTitle, barChart);

        // LineChart : Évolution dans le temps
        VBox lineBox = new VBox(10);
        lineBox.getStyleClass().add("card");
        HBox.setHgrow(lineBox, Priority.ALWAYS);
        lineBox.setPrefWidth(450);

        Label lineTitle = new Label("Évolution chronologique de la moyenne");
        lineTitle.getStyleClass().add("heading-md");

        CategoryAxis lineXAxis = new CategoryAxis();
        NumberAxis lineYAxis = new NumberAxis(0, 20, 2);
        lineYAxis.setLabel("Moyenne");
        LineChart<String, Number> lineChart = new LineChart<>(lineXAxis, lineYAxis);
        lineChart.setLegendVisible(false);
        lineChart.setAnimated(false);
        lineChart.setPrefHeight(260);

        if (engine.capabilities().averageTimeline()) {
            XYChart.Series<String, Number> timeSeries = new XYChart.Series<>();
            List<TimelinePoint> points = engine.getAverageTimeline(sem.id());
            for (TimelinePoint pt : points) {
                if (pt.value() instanceof ComputedResult.Value val) {
                    timeSeries.getData().add(new XYChart.Data<>(Formatters.formatShortDate(pt.date()), val.value()));
                }
            }
            lineChart.getData().add(timeSeries);
            lineBox.getChildren().addAll(lineTitle, lineChart);
        } else {
            lineBox.getChildren().addAll(lineTitle, new Label("Évolution chronologique non fournie par le moteur."));
        }

        chartsGrid.getChildren().addAll(barBox, lineBox);
        content.getChildren().add(chartsGrid);

        // 3. Section Simulateur de moyennes
        if (engine.capabilities().simulation()) {
            buildSimulatorSection(sem, engine);
        }
    }

    private void buildSimulatorSection(SemesterSummary sem, GradeEngine engine) {
        VBox simBox = new VBox(16);
        simBox.getStyleClass().add("card");

        HBox simHeader = new HBox(8);
        simHeader.setAlignment(Pos.CENTER_LEFT);
        Label simTitle = new Label("Simulateur d'estimations de notes");
        simTitle.getStyleClass().add("heading-lg");
        Label simBadge = new Label("Mode Simulation");
        simBadge.getStyleClass().addAll("badge", "badge-warning");
        simHeader.getChildren().addAll(simTitle, simBadge);

        Label simDesc = new Label("Attribuez des hypothèses de notes sur les évaluations sans note pour simuler vos résultats futurs. " +
                "Toute simulation est purement indicative et n'affecte pas vos vraies notes.");
        simDesc.getStyleClass().add("text-muted");
        simDesc.setWrapText(true);

        simBox.getChildren().addAll(simHeader, simDesc);

        // Liste des évaluations ouvertes (en attente ou planifiées)
        List<EvaluationSummary> openEvals = engine.getEvaluations(
                EvaluationFilter.all().withSemester(sem.id()).withStatus(EvaluationStatus.PLANNED, EvaluationStatus.AWAITING_GRADE)
        );

        if (openEvals.isEmpty()) {
            simBox.getChildren().add(new Label("Toutes les évaluations de ce semestre disposent déjà d'une note. Aucune simulation requise !"));
            content.getChildren().add(simBox);
            return;
        }

        GridPane simGrid = new GridPane();
        simGrid.setHgap(16);
        simGrid.setVgap(10);
        simGrid.setStyle("-fx-padding: 8px 0;");

        Map<EvaluationId, TextField> hypothesisInputs = new LinkedHashMap<>();

        int row = 0;
        for (EvaluationSummary ev : openEvals) {
            Label evLabel = new Label(ev.ue().code() + " — " + ev.title());
            evLabel.getStyleClass().add("heading-sm");

            Label maxLbl = new Label("sur " + String.format(Locale.FRENCH, "%.0f pts", ev.maxPoints()));
            maxLbl.getStyleClass().add("text-tertiary");

            TextField input = new TextField();
            input.setPromptText("Note simulée");
            input.setPrefWidth(120);

            hypothesisInputs.put(ev.id(), input);

            simGrid.add(evLabel, 0, row);
            simGrid.add(maxLbl, 1, row);
            simGrid.add(input, 2, row);
            row++;
        }

        // Résultats de la simulation
        VBox simResultBox = new VBox(8);
        simResultBox.setStyle("-fx-padding: 12px; -fx-background-color: -fx-bg-surface-alt; -fx-background-radius: 8px;");
        simResultBox.setVisible(false);

        Label simResAvg = new Label();
        simResAvg.getStyleClass().add("heading-md");
        simResultBox.getChildren().add(simResAvg);

        Button calcBtn = new Button("Calculer la simulation");
        calcBtn.getStyleClass().add("btn-primary");
        calcBtn.setOnAction(e -> {
            Map<EvaluationId, GradeEntry> hypotheses = new HashMap<>();
            for (Map.Entry<EvaluationId, TextField> entry : hypothesisInputs.entrySet()) {
                String txt = entry.getValue().getText().trim();
                if (!txt.isEmpty()) {
                    try {
                        double pts = Double.parseDouble(txt);
                        // Chercher le barème
                        double max = openEvals.stream().filter(ev -> ev.id().equals(entry.getKey())).findFirst().get().maxPoints();
                        hypotheses.put(entry.getKey(), GradeEntry.score(pts, max));
                    } catch (NumberFormatException ignored) {
                    }
                }
            }

            if (hypotheses.isEmpty()) {
                simResultBox.getChildren().setAll(new Label("Veuillez saisir au moins une hypothèse de note."));
                simResultBox.setVisible(true);
                return;
            }

            SimulationResult res = engine.simulate(new SimulationRequest(sem.id(), hypotheses));
            simResultBox.getChildren().clear();

            Label titleR = new Label("Résultats simulés :");
            titleR.getStyleClass().add("heading-sm");
            Label avgR = new Label("Moyenne générale estimée : " + Formatters.formatComputedResult(res.semesterAverage()));
            avgR.getStyleClass().add("heading-md");
            simResultBox.getChildren().addAll(titleR, avgR);

            for (String ass : res.assumptions()) {
                Label assLbl = new Label("• " + ass);
                assLbl.getStyleClass().add("text-muted");
                assLbl.setStyle("-fx-font-size: 11px;");
                simResultBox.getChildren().add(assLbl);
            }
            simResultBox.setVisible(true);
        });

        simBox.getChildren().addAll(simGrid, calcBtn, simResultBox);
        content.getChildren().add(simBox);
    }
}
