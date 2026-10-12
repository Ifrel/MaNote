package api.views.screens;

import api.controllers.AppController;
import api.views.dialogs.*;
import api.views.format.Formatters;
import api.views.widgets.EmptyState;
import api.views.widgets.StatusBadge;
import application.engine.GradeEngine;
import application.engine.model.AcademicYearSummary;
import application.engine.model.EvaluationProgress;
import application.engine.model.SemesterSummary;
import application.engine.model.UeSummary;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.List;

/**
 * Écran 2 — Semestres & Années universitaires (SemestersScreen).
 * Permet de créer, éditer, supprimer et structurer l'ensemble du cursus :
 * Années, Semestres, UE et Évaluations/DS.
 */
public class SemestersScreen extends ScrollPane {

    private final AppController controller;
    private final VBox content = new VBox(20);
    private final ComboBox<AcademicYearSummary> yearCombo = new ComboBox<>();

    public SemestersScreen(AppController controller) {
        this.controller = controller;

        setFitToWidth(true);
        setStyle("-fx-background-color: transparent;");
        content.setPadding(new Insets(24));
        setContent(content);

        buildHeader();
        controller.getContext().activeYearProperty().addListener((obs, oldV, newV) -> refresh());
        controller.getContext().activeSemesterProperty().addListener((obs, oldV, newV) -> refresh());
        refresh();
    }

    private void buildHeader() {
        HBox topBox = new HBox(12);
        topBox.setAlignment(Pos.CENTER_LEFT);

        VBox titleBox = new VBox(4);
        Label title = new Label("Parcours & Structure du Cursus");
        title.getStyleClass().add("heading-xl");
        Label subtitle = new Label("Gérez vos années, semestres, UE et préparez vos DS et contrôles.");
        subtitle.getStyleClass().add("text-muted");
        titleBox.getChildren().addAll(title, subtitle);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Sélecteur d'année
        yearCombo.setPrefWidth(220);
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

        yearCombo.valueProperty().addListener((obs, oldV, newV) -> {
            if (newV != null && !newV.equals(controller.getContext().getActiveYear())) {
                controller.selectYear(newV, this::refresh);
            }
        });

        // Bouton Ajouter Année
        Button addYearBtn = new Button("Nouvelle Année");
        addYearBtn.getStyleClass().add("btn-primary");
        FontIcon plusIcon = new FontIcon(Feather.PLUS);
        plusIcon.setIconSize(13);
        addYearBtn.setGraphic(plusIcon);
        addYearBtn.setOnAction(e -> {
            YearDialog dlg = new YearDialog(null);
            dlg.showAndWait().ifPresent(draft -> controller.createAcademicYear(draft, id -> refresh()));
        });

        // Boutons YAML
        Button importYamlBtn = new Button("Importer YAML");
        importYamlBtn.getStyleClass().add("btn-secondary");
        FontIcon fileIcon = new FontIcon(Feather.FILE_TEXT);
        fileIcon.setIconSize(13);
        importYamlBtn.setGraphic(fileIcon);
        importYamlBtn.setOnAction(e -> controller.syncCurriculumFromYaml(this::refresh));

        Button exportYamlBtn = new Button("Exporter YAML");
        exportYamlBtn.getStyleClass().add("btn-secondary");
        FontIcon downloadIcon = new FontIcon(Feather.DOWNLOAD);
        downloadIcon.setIconSize(13);
        exportYamlBtn.setGraphic(downloadIcon);
        exportYamlBtn.setOnAction(e -> controller.exportCurriculumToYaml(null));

        topBox.getChildren().addAll(titleBox, spacer, new Label("Année :"), yearCombo, addYearBtn, importYamlBtn, exportYamlBtn);
        content.getChildren().add(topBox);
    }

    public void refresh() {
        GradeEngine engine = controller.getEngine();
        List<AcademicYearSummary> years = engine.getAcademicYears();
        yearCombo.getItems().setAll(years);

        AcademicYearSummary currentYear = controller.getContext().getActiveYear();
        if (currentYear != null) {
            yearCombo.setValue(currentYear);
        } else if (!years.isEmpty()) {
            yearCombo.setValue(years.get(0));
            currentYear = years.get(0);
        }

        // Supprimer les anciens composants (conserver le topBox)
        while (content.getChildren().size() > 1) {
            content.getChildren().remove(1);
        }

        if (currentYear == null) {
            content.getChildren().add(new EmptyState(
                    Feather.BOOK_OPEN,
                    "Aucune année universitaire configurée",
                    "Votre cursus est pour le moment vierge. Créez votre première année ou importez un fichier cursus.yaml.",
                    "Créer une Année Universitaire",
                    () -> {
                        YearDialog dlg = new YearDialog(null);
                        dlg.showAndWait().ifPresent(draft -> controller.createAcademicYear(draft, id -> refresh()));
                    }
            ));
            return;
        }

        // 1. Carte Année globale avec boutons de gestion
        HBox yearCard = new HBox(16);
        yearCard.getStyleClass().add("card");
        yearCard.setAlignment(Pos.CENTER_LEFT);

        VBox yInfo = new VBox(4);
        Label yTitle = new Label(currentYear.label());
        yTitle.getStyleClass().add("heading-md");
        Label yProg = new Label("Filière : " + currentYear.programme());
        yProg.getStyleClass().add("text-muted");
        yInfo.getChildren().addAll(yTitle, yProg);

        Region ySpacer = new Region();
        HBox.setHgrow(ySpacer, Priority.ALWAYS);

        VBox yScoreBox = new VBox(2);
        yScoreBox.setAlignment(Pos.CENTER_RIGHT);
        Label yAvg = new Label("Moyenne annuelle : " + Formatters.formatComputedResult(currentYear.average()));
        yAvg.getStyleClass().add("heading-sm");
        StatusBadge yBadge = StatusBadge.ofValidation(currentYear.validation());
        yScoreBox.getChildren().addAll(yAvg, yBadge);

        // Bouton Nouveau Semestre
        final AcademicYearSummary fYear = currentYear;
        Button addSemBtn = new Button("Nouveau Semestre");
        addSemBtn.getStyleClass().add("btn-primary");
        FontIcon plusIcon = new FontIcon(Feather.PLUS);
        plusIcon.setIconSize(13);
        addSemBtn.setGraphic(plusIcon);
        addSemBtn.setOnAction(e -> {
            SemesterDialog dlg = new SemesterDialog(years, fYear.id(), null);
            dlg.showAndWait().ifPresent(draft -> controller.createSemester(draft, id -> refresh()));
        });

        // Bouton Supprimer Année
        Button delYearBtn = new Button();
        delYearBtn.getStyleClass().add("btn-icon");
        FontIcon trashIcon = new FontIcon(Feather.TRASH_2);
        trashIcon.setIconSize(14);
        delYearBtn.setGraphic(trashIcon);
        delYearBtn.setOnAction(e -> {
            if (ConfirmationDialog.confirm("Supprimer l'année",
                    "Supprimer l'année \"" + fYear.label() + "\" ?",
                    "Tous les semestres et UE associés seront supprimés.")) {
                controller.deleteAcademicYear(fYear.id(), this::refresh);
            }
        });

        yearCard.getChildren().addAll(yInfo, ySpacer, yScoreBox, addSemBtn, delYearBtn);
        content.getChildren().add(yearCard);

        // 2. Liste des semestres
        List<SemesterSummary> semesters = engine.getSemesters(currentYear.id());
        if (semesters.isEmpty()) {
            content.getChildren().add(new EmptyState(
                    Feather.CALENDAR,
                    "Aucun semestre pour cette année",
                    "Ajoutez votre premier semestre pour commencer à renseigner vos UE et notes.",
                    "Ajouter un Semestre",
                    () -> {
                        SemesterDialog dlg = new SemesterDialog(years, fYear.id(), null);
                        dlg.showAndWait().ifPresent(draft -> controller.createSemester(draft, id -> refresh()));
                    }
            ));
            return;
        }

        for (SemesterSummary sem : semesters) {
            content.getChildren().add(buildSemesterCard(sem, semesters));
        }
    }

    private VBox buildSemesterCard(SemesterSummary sem, List<SemesterSummary> allSemesters) {
        VBox card = new VBox(14);
        card.getStyleClass().add("card");

        boolean isActive = controller.getContext().getActiveSemester() != null
                && sem.id().equals(controller.getContext().getActiveSemester().id());

        // En-tête du semestre
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);

        Label semTitle = new Label(sem.label());
        semTitle.getStyleClass().add("heading-md");

        Label creditBadge = new Label(sem.totalCredits() + " ECTS • " + sem.ueCount() + " UE");
        creditBadge.getStyleClass().addAll("badge", "badge-neutral");

        StatusBadge valBadge = StatusBadge.ofValidation(sem.validation());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button selectBtn = new Button(isActive ? "Semestre actif" : "Définir comme actif");
        selectBtn.getStyleClass().add(isActive ? "btn-secondary" : "btn-primary");
        selectBtn.setDisable(isActive);
        selectBtn.setOnAction(e -> controller.selectSemester(sem, this::refresh));

        // Bouton Ajouter UE
        Button addUeBtn = new Button("+ UE");
        addUeBtn.getStyleClass().add("btn-secondary");
        addUeBtn.setOnAction(e -> {
            UeDialog dlg = new UeDialog(allSemesters, sem.id(), null);
            dlg.showAndWait().ifPresent(draft -> controller.createUe(draft, id -> refresh()));
        });

        // Bouton Supprimer Semestre
        Button delSemBtn = new Button();
        delSemBtn.getStyleClass().add("btn-icon");
        FontIcon trash = new FontIcon(Feather.TRASH_2);
        trash.setIconSize(14);
        delSemBtn.setGraphic(trash);
        delSemBtn.setOnAction(e -> {
            if (ConfirmationDialog.confirm("Supprimer le semestre",
                    "Supprimer \"" + sem.label() + "\" ?",
                    "Toutes les UE et notes de ce semestre seront supprimées.")) {
                controller.deleteSemester(sem.id(), this::refresh);
            }
        });

        header.getChildren().addAll(semTitle, creditBadge, valBadge, spacer, addUeBtn, selectBtn, delSemBtn);

        // Ligne récapitulative
        HBox statsRow = new HBox(24);
        statsRow.setAlignment(Pos.CENTER_LEFT);
        statsRow.setStyle("-fx-padding: 8px 0;");

        Label avgLabel = new Label("Moyenne semestrielle : " + Formatters.formatComputedResult(sem.average()));
        avgLabel.getStyleClass().add("heading-sm");

        EvaluationProgress p = sem.progress();
        Label progLabel = new Label(String.format("Évaluations : %d notée(s) sur %d (%d en attente)",
                p.graded(), p.total(), p.awaitingGrade()));
        progLabel.getStyleClass().add("text-muted");

        statsRow.getChildren().addAll(avgLabel, progLabel);

        // Liste des UE de ce semestre
        VBox ueList = new VBox(8);
        List<UeSummary> ues = controller.getEngine().getUes(sem.id());

        if (ues.isEmpty()) {
            Label noUe = new Label("Aucune UE dans ce semestre. Cliquez sur '+ UE' pour en ajouter une.");
            noUe.getStyleClass().add("text-muted");
            noUe.setStyle("-fx-padding: 8px 0;");
            ueList.getChildren().add(noUe);
        } else {
            for (UeSummary ue : ues) {
                HBox ueRow = new HBox(12);
                ueRow.setAlignment(Pos.CENTER_LEFT);
                ueRow.setStyle("-fx-padding: 8px 12px; -fx-background-color: -fx-bg-surface-alt; -fx-background-radius: 6px;");

                Label code = new Label(ue.code());
                code.getStyleClass().add("heading-sm");
                code.setPrefWidth(90);

                Label name = new Label(ue.name() + " (" + ue.credits() + " ECTS)");
                HBox.setHgrow(name, Priority.ALWAYS);

                Label score = new Label(Formatters.formatComputedResult(ue.average()));
                score.getStyleClass().add("heading-sm");

                StatusBadge badge = StatusBadge.ofValidation(ue.validation());

                // Bouton Consulter l'UE
                Button viewBtn = new Button("Détails");
                viewBtn.getStyleClass().add("btn-secondary");
                viewBtn.setOnAction(e -> controller.navigateToUe(ue.id()));

                // Bouton Ajouter DS / Évaluation directement
                Button addEvalBtn = new Button("+ DS");
                addEvalBtn.getStyleClass().add("btn-secondary");
                addEvalBtn.setOnAction(e -> {
                    EvaluationDialog dlg = new EvaluationDialog(controller.getEngine(), sem.id(), null);
                    dlg.showAndWait().ifPresent(draft -> controller.createEvaluation(draft, id -> refresh()));
                });

                // Bouton Supprimer UE
                Button delUeBtn = new Button();
                delUeBtn.getStyleClass().add("btn-icon");
                FontIcon ueTrash = new FontIcon(Feather.TRASH_2);
                ueTrash.setIconSize(13);
                delUeBtn.setGraphic(ueTrash);
                delUeBtn.setOnAction(e -> {
                    if (ConfirmationDialog.confirm("Supprimer l'UE",
                            "Supprimer l'UE " + ue.code() + " ?",
                            "Toutes les notes et DS associés seront supprimés.")) {
                        controller.deleteUe(ue.id(), this::refresh);
                    }
                });

                ueRow.getChildren().addAll(code, name, score, badge, addEvalBtn, viewBtn, delUeBtn);
                ueList.getChildren().add(ueRow);
            }
        }

        card.getChildren().addAll(header, statsRow, ueList);
        return card;
    }
}
