package api.views.screens;

import api.controllers.AppController;
import api.views.dialogs.GradeDialog;
import api.views.format.Formatters;
import api.views.widgets.EmptyState;
import api.views.widgets.StatusBadge;
import application.engine.GradeEngine;
import application.engine.model.EvaluationFilter;
import application.engine.model.EvaluationStatus;
import application.engine.model.EvaluationSummary;
import application.engine.model.SemesterSummary;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

/**
 * Écran 8 — Planification & Échéances (PlanningScreen).
 * Regroupe les évaluations à venir par horizon temporel et repère les semaines chargées.
 */
public class PlanningScreen extends ScrollPane {

    private final AppController controller;
    private final VBox content = new VBox(20);

    public PlanningScreen(AppController controller) {
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
                    Feather.CLOCK,
                    "Aucun semestre sélectionné",
                    "Sélectionnez un semestre pour consulter l'échéancier.",
                    null, null
            ));
            return;
        }

        GradeEngine engine = controller.getEngine();

        // 1. En-tête
        VBox titleBox = new VBox(4);
        Label title = new Label("Planification & Prochaines Échéances");
        title.getStyleClass().add("heading-xl");
        Label subtitle = new Label("Anticipez vos révisions et contrôles à venir pour le " + sem.label() + ".");
        subtitle.getStyleClass().add("text-muted");
        titleBox.getChildren().addAll(title, subtitle);
        content.getChildren().add(titleBox);

        List<EvaluationSummary> evals = engine.getEvaluations(
                EvaluationFilter.all().withSemester(sem.id())
        );

        LocalDate today = LocalDate.now();
        LocalDate in7Days = today.plusDays(7);
        LocalDate in30Days = today.plusDays(30);

        List<EvaluationSummary> imminent = evals.stream()
                .filter(e -> e.status().isOpen() && e.date().isPresent() && !e.date().get().isBefore(today) && !e.date().get().isAfter(in7Days))
                .sorted(Comparator.comparing(e -> e.date().get()))
                .toList();

        List<EvaluationSummary> upcomingMonth = evals.stream()
                .filter(e -> e.status().isOpen() && e.date().isPresent() && e.date().get().isAfter(in7Days) && !e.date().get().isAfter(in30Days))
                .sorted(Comparator.comparing(e -> e.date().get()))
                .toList();

        List<EvaluationSummary> later = evals.stream()
                .filter(e -> e.status().isOpen() && e.date().isPresent() && e.date().get().isAfter(in30Days))
                .sorted(Comparator.comparing(e -> e.date().get()))
                .toList();

        List<EvaluationSummary> awaiting = evals.stream()
                .filter(e -> e.status() == EvaluationStatus.AWAITING_GRADE)
                .sorted(Comparator.comparing(e -> e.date().orElse(LocalDate.MIN)))
                .toList();

        List<EvaluationSummary> undated = evals.stream()
                .filter(e -> e.status().isOpen() && e.date().isEmpty())
                .toList();

        // Section Imminent (Dans les 7 jours)
        content.getChildren().add(buildSection("Échéances très proches (7 prochains jours)", imminent, "badge-warning", Feather.ALERT_CIRCLE));

        // Section Notes en attente
        content.getChildren().add(buildSection("Évaluations passées — Notes en attente", awaiting, "badge-info", Feather.CLOCK));

        // Section Dans le mois
        content.getChildren().add(buildSection("Dans les 30 prochains jours", upcomingMonth, "badge-neutral", Feather.CALENDAR));

        // Section Plus tard
        content.getChildren().add(buildSection("Plus tard dans le semestre", later, "badge-neutral", Feather.CALENDAR));

        // Section Dates non fixées
        if (!undated.isEmpty()) {
            content.getChildren().add(buildSection("Date non encore communiquée", undated, "badge-neutral", Feather.HELP_CIRCLE));
        }
    }

    private VBox buildSection(String title, List<EvaluationSummary> items, String badgeStyle, Feather icon) {
        VBox section = new VBox(10);
        HBox secHeader = new HBox(8);
        secHeader.setAlignment(Pos.CENTER_LEFT);

        FontIcon fIcon = new FontIcon(icon);
        fIcon.setIconSize(16);
        Label secTitle = new Label(title);
        secTitle.getStyleClass().add("heading-md");
        Label countBadge = new Label(String.valueOf(items.size()));
        countBadge.getStyleClass().addAll("badge", badgeStyle);

        secHeader.getChildren().addAll(fIcon, secTitle, countBadge);
        section.getChildren().add(secHeader);

        if (items.isEmpty()) {
            Label empty = new Label("Aucune évaluation dans cette période.");
            empty.getStyleClass().add("text-tertiary");
            empty.setStyle("-fx-padding: 8px 12px;");
            section.getChildren().add(empty);
        } else {
            for (EvaluationSummary ev : items) {
                HBox card = new HBox(16);
                card.getStyleClass().add("card");
                card.setAlignment(Pos.CENTER_LEFT);
                card.setStyle("-fx-padding: 12px 16px;");

                VBox left = new VBox(2);
                Label t = new Label(ev.title());
                t.getStyleClass().add("heading-sm");
                Label sub = new Label(ev.ue().code() + " — " + ev.ue().name() + " • " + ev.type().label());
                sub.getStyleClass().add("text-muted");
                left.getChildren().addAll(t, sub);

                Region sp = new Region();
                HBox.setHgrow(sp, Priority.ALWAYS);

                Label dateLbl = new Label(Formatters.formatDate(ev.date()));
                dateLbl.getStyleClass().add("heading-sm");

                StatusBadge badge = StatusBadge.ofEvaluationStatus(ev.status());

                Button gradeBtn = new Button("Saisir note");
                gradeBtn.getStyleClass().add("btn-secondary");
                gradeBtn.setOnAction(e -> {
                    GradeDialog dlg = new GradeDialog(ev);
                    dlg.showAndWait().ifPresent(res -> {
                        if (res instanceof GradeDialog.GradeDialogResult.Save s) {
                            controller.recordGrade(ev.id(), s.entry(), this::refresh);
                        }
                    });
                });

                card.getChildren().addAll(left, sp, dateLbl, badge, gradeBtn);
                section.getChildren().add(card);
            }
        }
        return section;
    }
}
