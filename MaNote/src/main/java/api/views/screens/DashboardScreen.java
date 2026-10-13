package api.views.screens;

import api.controllers.AppController;
import api.views.format.Formatters;
import api.views.widgets.EmptyState;
import api.views.widgets.ScoreRing;
import api.views.widgets.StatusBadge;
import api.views.widgets.UECard;
import application.engine.GradeEngine;
import application.engine.model.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.*;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

/**
 * Écran 1 — Tableau de bord.
 * Un seul élément dominant (l'anneau de moyenne du semestre), le reste reste sobre.
 */
public class DashboardScreen extends ScrollPane {

    private static final double PAGE_PADDING = 28;
    private static final double GRID_GAP = 16;
    private static final double CARD_MIN_WIDTH = 270;

    private final AppController controller;
    private final VBox content = new VBox(26);
    private FlowPane ueGrid;

    public DashboardScreen(AppController controller) {
        this.controller = controller;

        setFitToWidth(true);
        setStyle("-fx-background-color: transparent; -fx-padding: 0;");
        content.setPadding(new Insets(PAGE_PADDING));
        setContent(content);

        controller.getContext().activeSemesterProperty().addListener((obs, oldV, newV) -> refresh());
        viewportBoundsProperty().addListener((obs, oldV, newV) -> relayoutGrid());
        refresh();
    }

    public void refresh() {
        content.getChildren().clear();
        ueGrid = null;
        SemesterSummary sem = controller.getContext().getActiveSemester();
        AcademicYearSummary year = controller.getContext().getActiveYear();

        if (year == null) {
            content.getChildren().add(new EmptyState(
                    Feather.BOOK_OPEN,
                    "Bienvenue dans MaNote",
                    "Votre base est vide pour l'instant. Créez votre première année ou importez votre fichier cursus.yaml.",
                    "Créer une année universitaire",
                    () -> {
                        api.views.dialogs.YearDialog dlg = new api.views.dialogs.YearDialog(null);
                        dlg.showAndWait().ifPresent(draft -> controller.createAcademicYear(draft, id -> refresh()));
                    }
            ));
            return;
        }

        if (sem == null) {
            content.getChildren().add(new EmptyState(
                    Feather.CALENDAR,
                    "Aucun semestre configuré",
                    "Ajoutez un premier semestre à « " + year.label() + " » pour saisir vos UE et vos notes.",
                    "Ajouter un semestre",
                    () -> {
                        api.views.dialogs.SemesterDialog dlg = new api.views.dialogs.SemesterDialog(
                                controller.getEngine().getAcademicYears(), year.id(), null);
                        dlg.showAndWait().ifPresent(draft -> controller.createSemester(draft, id -> refresh()));
                    }
            ));
            return;
        }

        GradeEngine engine = controller.getEngine();
        content.getChildren().addAll(
                buildHeader(year, sem),
                buildHero(sem),
                buildUeSection(engine, sem),
                buildBottom(engine, sem)
        );
        relayoutGrid();
    }

    // ------------------------------------------------------------------ en-tête & hero

    private Node buildHeader(AcademicYearSummary year, SemesterSummary sem) {
        VBox box = new VBox(4);
        Label title = new Label("Tableau de bord");
        title.getStyleClass().add("heading-xl");
        Label subtitle = new Label(year.programme() + " • " + year.label() + " • " + sem.label());
        subtitle.getStyleClass().add("text-muted");
        box.getChildren().addAll(title, subtitle);
        return box;
    }

    private Node buildHero(SemesterSummary sem) {
        ScoreRing ring = new ScoreRing(156, 13);
        ring.setResult(sem.average(), sem.validation().state());

        Label kicker = new Label("Moyenne du semestre");
        kicker.getStyleClass().add("stat-label");
        Label semName = new Label(sem.label());
        semName.getStyleClass().add("heading-lg");
        StatusBadge badge = StatusBadge.ofValidation(sem.validation());

        String detailText = !sem.validation().detail().isBlank()
                ? sem.validation().detail()
                : Formatters.formatComputedResult(sem.average());
        Label detail = new Label(detailText);
        detail.getStyleClass().add("text-muted");
        detail.setWrapText(true);

        VBox summary = new VBox(8, kicker, semName, new HBox(badge), detail);
        summary.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(summary, Priority.ALWAYS);

        HBox top = new HBox(30, ring, summary);
        top.setAlignment(Pos.CENTER_LEFT);

        EvaluationProgress prog = sem.progress();
        HBox metrics = new HBox();
        metrics.getStyleClass().add("hero-metrics");
        metrics.getChildren().addAll(
                metric("Évaluations notées", prog.graded() + " / " + prog.total(),
                        prog.total() > 0 ? String.format("%.0f %% du semestre", prog.gradedRatio() * 100) : "Aucune évaluation",
                        prog.total() > 0 ? prog.gradedRatio() : null, false),
                metric("Volume", sem.totalCredits() + " ECTS", sem.ueCount() + " UE", null, true),
                metric("À venir", prog.awaitingGrade() + " en attente", prog.planned() + " prévue(s)", null, true)
        );

        VBox hero = new VBox(22, top, metrics);
        hero.getStyleClass().add("hero");
        return hero;
    }

    private VBox metric(String label, String value, String caption, Double progress, boolean divided) {
        Label l = new Label(label);
        l.getStyleClass().add("stat-label");
        Label v = new Label(value);
        v.getStyleClass().add("metric-value");
        Label c = new Label(caption);
        c.getStyleClass().add("text-tertiary");
        c.setStyle("-fx-font-size: 11px;");

        VBox box = new VBox(3, l, v, c);
        if (progress != null) {
            ProgressBar bar = new ProgressBar(progress);
            bar.setMaxWidth(Double.MAX_VALUE);
            bar.setMinHeight(6);
            bar.setPrefHeight(6);
            VBox.setMargin(bar, new Insets(4, 0, 0, 0));
            box.getChildren().add(bar);
        }
        box.getStyleClass().add("metric");
        if (divided) box.getStyleClass().add("metric-divided");
        HBox.setHgrow(box, Priority.ALWAYS);
        box.setPrefWidth(0);
        box.setMaxWidth(Double.MAX_VALUE);
        return box;
    }

    // ------------------------------------------------------------------ UE

    private Node buildUeSection(GradeEngine engine, SemesterSummary sem) {
        VBox section = new VBox(14);

        List<UeSummary> ues = engine.getUes(sem.id());
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        Label title = new Label("Unités d'enseignement");
        title.getStyleClass().add("heading-lg");
        Label count = new Label(String.valueOf(ues.size()));
        count.getStyleClass().addAll("badge", "badge-neutral");
        header.getChildren().addAll(title, count);

        if (ues.isEmpty()) {
            Button addFirstUeBtn = new Button("Ajouter une UE");
            addFirstUeBtn.getStyleClass().add("btn-primary");
            addFirstUeBtn.setOnAction(e -> {
                api.views.dialogs.UeDialog dlg = new api.views.dialogs.UeDialog(List.of(sem), sem.id(), null);
                dlg.showAndWait().ifPresent(draft -> controller.createUe(draft, id -> refresh()));
            });
            Label hint = new Label("Aucune UE dans ce semestre pour le moment.");
            hint.getStyleClass().add("text-muted");
            VBox empty = new VBox(12, hint, addFirstUeBtn);
            empty.getStyleClass().add("card");
            empty.setAlignment(Pos.CENTER);
            empty.setStyle("-fx-padding: 28px;");
            section.getChildren().addAll(header, empty);
        } else {
            ueGrid = new FlowPane(GRID_GAP, GRID_GAP);
            for (UeSummary ue : ues) {
                UECard card = new UECard(ue, clicked -> controller.navigateToUe(clicked.id()));
                card.setPrefWidth(300);
                ueGrid.getChildren().add(card);
            }
            section.getChildren().addAll(header, ueGrid);
        }
        return section;
    }

    /**
     * Répartit les cartes d'UE en colonnes égales selon la largeur disponible.
     */
    private void relayoutGrid() {
        if (ueGrid == null) return;
        double available = getViewportBounds().getWidth() - 2 * PAGE_PADDING;
        if (available <= 0) return;
        int cols = Math.max(1, (int) Math.floor((available + GRID_GAP) / (CARD_MIN_WIDTH + GRID_GAP)));
        double width = (available - GRID_GAP * (cols - 1)) / cols;
        ueGrid.setPrefWrapLength(available);
        for (Node n : ueGrid.getChildren()) {
            if (n instanceof Region r) r.setPrefWidth(width);
        }
    }

    // ------------------------------------------------------------------ notes récentes & échéances

    private Node buildBottom(GradeEngine engine, SemesterSummary sem) {
        HBox bottom = new HBox(20);

        // Dernières notes
        VBox gradesCard = new VBox();
        gradesCard.getStyleClass().add("card");
        List<EvaluationSummary> graded = engine.getEvaluations(
                EvaluationFilter.all().withSemester(sem.id()).withStatus(EvaluationStatus.GRADED));
        graded.sort(Comparator.comparing((EvaluationSummary e) -> e.date().orElse(LocalDate.MIN)).reversed());

        if (graded.isEmpty()) {
            gradesCard.getChildren().add(emptyHint("Aucune note enregistrée pour ce semestre."));
        } else {
            for (int i = 0; i < Math.min(5, graded.size()); i++) {
                EvaluationSummary ev = graded.get(i);
                Label score = new Label(Formatters.formatGrade(ev.grade(), ev.maxPoints()));
                score.getStyleClass().add("heading-md");
                gradesCard.getChildren().add(listRow(null, ev.title(),
                        ev.ue().code() + " • " + Formatters.formatDate(ev.date()), score, i == 0));
            }
        }

        // Prochaines échéances
        VBox upcomingCard = new VBox();
        upcomingCard.getStyleClass().add("card");
        List<EvaluationSummary> upcoming = engine.getEvaluations(
                EvaluationFilter.all().withSemester(sem.id())
                        .withStatus(EvaluationStatus.PLANNED, EvaluationStatus.AWAITING_GRADE));
        upcoming.sort(Comparator.comparing(e -> e.date().orElse(LocalDate.MAX)));

        if (upcoming.isEmpty()) {
            upcomingCard.getChildren().add(emptyHint("Aucune évaluation prévue pour le moment."));
        } else {
            for (int i = 0; i < Math.min(5, upcoming.size()); i++) {
                EvaluationSummary ev = upcoming.get(i);
                upcomingCard.getChildren().add(listRow(iconChip(Feather.CALENDAR), ev.title(),
                        ev.ue().code() + " • " + Formatters.formatDate(ev.date()),
                        StatusBadge.ofEvaluationStatus(ev.status()), i == 0));
            }
        }

        bottom.getChildren().addAll(
                panel("Dernières notes", gradesCard),
                panel("Prochaines échéances", upcomingCard));
        return bottom;
    }

    private VBox panel(String title, Node body) {
        Label t = new Label(title);
        t.getStyleClass().add("heading-md");
        VBox box = new VBox(12, t, body);
        HBox.setHgrow(box, Priority.ALWAYS);
        box.setPrefWidth(0);
        box.setMaxWidth(Double.MAX_VALUE);
        return box;
    }

    private Label emptyHint(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("text-muted");
        l.setStyle("-fx-padding: 12px 0;");
        return l;
    }

    private Node iconChip(Feather icon) {
        FontIcon fi = new FontIcon(icon);
        fi.setIconSize(16);
        StackPane chip = new StackPane(fi);
        chip.getStyleClass().add("icon-chip");
        return chip;
    }

    private HBox listRow(Node leading, String title, String sub, Node trailing, boolean first) {
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("list-row");
        if (first) row.getStyleClass().add("list-row-first");

        Label t = new Label(title);
        t.getStyleClass().add("heading-sm");
        Label s = new Label(sub);
        s.getStyleClass().add("text-tertiary");
        VBox info = new VBox(2, t, s);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        if (leading != null) row.getChildren().add(leading);
        row.getChildren().addAll(info, spacer, trailing);
        return row;
    }
}
