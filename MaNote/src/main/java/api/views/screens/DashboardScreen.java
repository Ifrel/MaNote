package api.views.screens;

import api.controllers.AppController;
import api.views.format.Formatters;
import api.views.widgets.EmptyState;
import api.views.widgets.StatCard;
import api.views.widgets.StatusBadge;
import api.views.widgets.UECard;
import application.engine.GradeEngine;
import application.engine.model.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.*;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

/**
 * Écran 1 — Tableau de bord (DashboardScreen).
 * Vue synthétique de la situation académique de l'étudiant.
 */
public class DashboardScreen extends ScrollPane {

    private final AppController controller;
    private final VBox content = new VBox(24);

    public DashboardScreen(AppController controller) {
        this.controller = controller;

        setFitToWidth(true);
        setStyle("-fx-background-color: transparent; -fx-padding: 0;");
        content.setPadding(new Insets(24));
        setContent(content);

        // Recharger quand le semestre actif change
        controller.getContext().activeSemesterProperty().addListener((obs, oldV, newV) -> refresh());
        refresh();
    }

    public void refresh() {
        content.getChildren().clear();
        SemesterSummary sem = controller.getContext().getActiveSemester();
        AcademicYearSummary year = controller.getContext().getActiveYear();

        if (year == null) {
            content.getChildren().add(new EmptyState(
                    Feather.BOOK_OPEN,
                    "Bienvenue dans MaNote !",
                    "Votre base de données est actuellement vierge. Vous pouvez commencer en créant votre première année ou en important votre fichier cursus.yaml.",
                    "Créer une Année Universitaire",
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
                    "Ajoutez un premier semestre à l'année \"" + year.label() + "\" pour commencer à saisir vos UE et notes.",
                    "Ajouter un Semestre",
                    () -> {
                        api.views.dialogs.SemesterDialog dlg = new api.views.dialogs.SemesterDialog(
                                controller.getEngine().getAcademicYears(), year.id(), null);
                        dlg.showAndWait().ifPresent(draft -> controller.createSemester(draft, id -> refresh()));
                    }
            ));
            return;
        }

        GradeEngine engine = controller.getEngine();

        // 1. En-tête de bienvenue
        VBox headerBox = new VBox(4);
        Label welcomeLabel = new Label("Tableau de bord académique");
        welcomeLabel.getStyleClass().add("heading-xl");

        String subtitleText = (year != null ? year.programme() + " • " + year.label() + " • " : "") + sem.label();
        Label subtitleLabel = new Label(subtitleText);
        subtitleLabel.getStyleClass().add("text-muted");
        headerBox.getChildren().addAll(welcomeLabel, subtitleLabel);
        content.getChildren().add(headerBox);

        // 2. Grille de StatCards
        HBox kpiGrid = new HBox(16);
        kpiGrid.setAlignment(Pos.CENTER_LEFT);

        // KPI 1: Moyenne générale calculée par le moteur
        String avgText = Formatters.formatComputedResult(sem.average());
        StatCard avgCard = new StatCard("Moyenne Semestrielle", avgText,
                sem.validation().label(), Feather.AWARD);
        avgCard.setBadge(StatusBadge.ofValidation(sem.validation()));
        HBox.setHgrow(avgCard, Priority.ALWAYS);

        // KPI 2: Progression des évaluations
        EvaluationProgress prog = sem.progress();
        String progText = String.format("%d / %d", prog.graded(), prog.total());
        String progSub = prog.total() > 0 ? String.format("%.0f%% des évaluations notées", prog.gradedRatio() * 100) : "Aucune évaluation";
        StatCard progCard = new StatCard("Progression", progText, progSub, Feather.CHECK_CIRCLE);
        HBox.setHgrow(progCard, Priority.ALWAYS);

        // KPI 3: Crédits du semestre
        StatCard creditCard = new StatCard("Volume ECTS", sem.totalCredits() + " ECTS",
                sem.ueCount() + " Unités d'Enseignement", Feather.BOOK_OPEN);
        HBox.setHgrow(creditCard, Priority.ALWAYS);

        // KPI 4: Notes en attente
        String pendingText = prog.awaitingGrade() + " en attente";
        String pendingSub = prog.planned() + " prévue(s) à venir";
        StatCard pendingCard = new StatCard("Échéances & Attente", pendingText, pendingSub, Feather.CLOCK);
        HBox.setHgrow(pendingCard, Priority.ALWAYS);

        kpiGrid.getChildren().addAll(avgCard, progCard, creditCard, pendingCard);
        content.getChildren().add(kpiGrid);

        // 3. Section UE
        VBox ueSection = new VBox(12);
        HBox ueHeader = new HBox(8);
        ueHeader.setAlignment(Pos.CENTER_LEFT);
        Label ueTitle = new Label("Unités d'Enseignement (UE)");
        ueTitle.getStyleClass().add("heading-lg");
        ueHeader.getChildren().add(ueTitle);

        List<UeSummary> ues = engine.getUes(sem.id());
        if (ues.isEmpty()) {
            Button addFirstUeBtn = new Button("Ajouter votre première UE");
            addFirstUeBtn.getStyleClass().add("btn-primary");
            addFirstUeBtn.setOnAction(e -> {
                api.views.dialogs.UeDialog dlg = new api.views.dialogs.UeDialog(List.of(sem), sem.id(), null);
                dlg.showAndWait().ifPresent(draft -> controller.createUe(draft, id -> refresh()));
            });
            VBox emptyUeBox = new VBox(10, new Label("Aucune Unité d'Enseignement pour le moment dans ce semestre."), addFirstUeBtn);
            emptyUeBox.getStyleClass().add("card");
            emptyUeBox.setAlignment(Pos.CENTER);
            emptyUeBox.setStyle("-fx-padding: 24px;");
            ueSection.getChildren().addAll(ueHeader, emptyUeBox);
        } else {
            FlowPane ueGrid = new FlowPane(16, 16);
            ueGrid.setPrefWrapLength(900);
            for (UeSummary ue : ues) {
                UECard card = new UECard(ue, clickedUe -> controller.navigateToUe(clickedUe.id()));
                card.setPrefWidth(280);
                ueGrid.getChildren().add(card);
            }
            ueSection.getChildren().addAll(ueHeader, ueGrid);
        }
        content.getChildren().add(ueSection);

        // 4. Deux colonnes : Dernières notes obtenues & Prochaines échéances
        HBox bottomSection = new HBox(20);

        // Colonne Gauche : Dernières notes
        VBox gradesCol = new VBox(12);
        HBox.setHgrow(gradesCol, Priority.ALWAYS);
        Label gradesTitle = new Label("Dernières notes enregistrées");
        gradesTitle.getStyleClass().add("heading-md");
        gradesCol.getChildren().add(gradesTitle);

        VBox gradesCard = new VBox(10);
        gradesCard.getStyleClass().add("card");

        List<EvaluationSummary> gradedEvals = engine.getEvaluations(
                EvaluationFilter.all().withSemester(sem.id()).withStatus(EvaluationStatus.GRADED)
        );
        gradedEvals.sort(Comparator.comparing((EvaluationSummary e) -> e.date().orElse(LocalDate.MIN)).reversed());

        if (gradedEvals.isEmpty()) {
            gradesCard.getChildren().add(new Label("Aucune note encore enregistrée pour ce semestre."));
        } else {
            for (int i = 0; i < Math.min(5, gradedEvals.size()); i++) {
                EvaluationSummary ev = gradedEvals.get(i);
                HBox row = new HBox(12);
                row.setAlignment(Pos.CENTER_LEFT);
                row.setStyle("-fx-padding: 8px 0; -fx-border-color: -fx-border-subtle transparent transparent transparent; -fx-border-width: 1px;");

                VBox info = new VBox(2);
                Label t = new Label(ev.title());
                t.getStyleClass().add("heading-sm");
                Label sub = new Label(ev.ue().code() + " • " + Formatters.formatDate(ev.date()));
                sub.getStyleClass().add("text-tertiary");
                info.getChildren().addAll(t, sub);

                Region sp = new Region();
                HBox.setHgrow(sp, Priority.ALWAYS);

                Label score = new Label(Formatters.formatGrade(ev.grade(), ev.maxPoints()));
                score.getStyleClass().add("heading-md");

                row.getChildren().addAll(info, sp, score);
                gradesCard.getChildren().add(row);
            }
        }
        gradesCol.getChildren().add(gradesCard);

        // Colonne Droite : Prochaines échéances
        VBox upcomingCol = new VBox(12);
        HBox.setHgrow(upcomingCol, Priority.ALWAYS);
        Label upcomingTitle = new Label("Prochaines échéances");
        upcomingTitle.getStyleClass().add("heading-md");
        upcomingCol.getChildren().add(upcomingTitle);

        VBox upcomingCard = new VBox(10);
        upcomingCard.getStyleClass().add("card");

        List<EvaluationSummary> upcomingEvals = engine.getEvaluations(
                EvaluationFilter.all().withSemester(sem.id()).withStatus(EvaluationStatus.PLANNED, EvaluationStatus.AWAITING_GRADE)
        );
        upcomingEvals.sort(Comparator.comparing(e -> e.date().orElse(LocalDate.MAX)));

        if (upcomingEvals.isEmpty()) {
            upcomingCard.getChildren().add(new Label("Aucune évaluation prévue pour le moment."));
        } else {
            for (int i = 0; i < Math.min(5, upcomingEvals.size()); i++) {
                EvaluationSummary ev = upcomingEvals.get(i);
                HBox row = new HBox(12);
                row.setAlignment(Pos.CENTER_LEFT);
                row.setStyle("-fx-padding: 8px 0; -fx-border-color: -fx-border-subtle transparent transparent transparent; -fx-border-width: 1px;");

                FontIcon calIcon = new FontIcon(Feather.CALENDAR);
                calIcon.getStyleClass().add("text-tertiary");

                VBox info = new VBox(2);
                Label t = new Label(ev.title());
                t.getStyleClass().add("heading-sm");
                Label sub = new Label(ev.ue().code() + " • " + Formatters.formatDate(ev.date()));
                sub.getStyleClass().add("text-tertiary");
                info.getChildren().addAll(t, sub);

                Region sp = new Region();
                HBox.setHgrow(sp, Priority.ALWAYS);

                StatusBadge b = StatusBadge.ofEvaluationStatus(ev.status());

                row.getChildren().addAll(calIcon, info, sp, b);
                upcomingCard.getChildren().add(row);
            }
        }
        upcomingCol.getChildren().add(upcomingCard);

        bottomSection.getChildren().addAll(gradesCol, upcomingCol);
        content.getChildren().add(bottomSection);
    }
}
