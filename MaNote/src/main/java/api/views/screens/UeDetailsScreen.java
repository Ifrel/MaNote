package api.views.screens;

import api.controllers.AppController;
import api.views.dialogs.ConfirmationDialog;
import api.views.dialogs.EvaluationDialog;
import api.views.dialogs.GradeDialog;
import api.views.format.Formatters;
import api.views.widgets.EmptyState;
import api.views.widgets.StatusBadge;
import application.engine.GradeEngine;
import application.engine.model.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.List;

/**
 * Écran 3 — Vue détaillée d'une UE (UeDetailsScreen).
 * Présente le détail complet, les notes, coefficients, contributions et explications.
 */
public class UeDetailsScreen extends ScrollPane {

    private final AppController controller;
    private final VBox content = new VBox(20);

    public UeDetailsScreen(AppController controller) {
        this.controller = controller;

        setFitToWidth(true);
        setStyle("-fx-background-color: transparent;");
        content.setPadding(new Insets(24));
        setContent(content);

        controller.getContext().selectedUeIdProperty().addListener((obs, oldV, newV) -> refresh());
        controller.getContext().activeSemesterProperty().addListener((obs, oldV, newV) -> refresh());
        refresh();
    }

    @SuppressWarnings("unchecked")
    public void refresh() {
        content.getChildren().clear();
        UeId ueId = controller.getContext().getSelectedUeId();

        if (ueId == null) {
            content.getChildren().add(new EmptyState(
                    Feather.BOOK_OPEN,
                    "Aucune UE sélectionnée",
                    "Sélectionnez une UE depuis le tableau de bord ou la liste des semestres.",
                    "Aller au tableau de bord",
                    () -> controller.navigateTo(api.model.NavigationDestination.DASHBOARD)
            ));
            return;
        }

        GradeEngine engine = controller.getEngine();
        UeDetails details;
        try {
            details = engine.getUe(ueId);
        } catch (Exception e) {
            content.getChildren().add(new EmptyState(
                    Feather.ALERT_CIRCLE,
                    "Impossible de charger l'UE",
                    e.getMessage(),
                    "Retour au tableau de bord",
                    () -> controller.navigateTo(api.model.NavigationDestination.DASHBOARD)
            ));
            return;
        }

        UeSummary sum = details.summary();

        // 1. En-tête de l'UE
        HBox header = new HBox(16);
        header.setAlignment(Pos.CENTER_LEFT);

        Button backBtn = new Button();
        backBtn.getStyleClass().add("btn-icon");
        FontIcon backIcon = new FontIcon(Feather.ARROW_LEFT);
        backIcon.setIconSize(18);
        backBtn.setGraphic(backIcon);
        backBtn.setOnAction(e -> controller.navigateTo(api.model.NavigationDestination.DASHBOARD));

        VBox titleBox = new VBox(4);
        HBox titleRow = new HBox(10);
        titleRow.setAlignment(Pos.CENTER_LEFT);

        Label codeLabel = new Label(sum.code());
        codeLabel.getStyleClass().add("heading-xl");

        Label creditBadge = new Label(sum.credits() + " ECTS");
        creditBadge.getStyleClass().addAll("badge", "badge-neutral");

        StatusBadge valBadge = StatusBadge.ofValidation(sum.validation());

        titleRow.getChildren().addAll(codeLabel, creditBadge, valBadge);

        Label nameLabel = new Label(sum.name());
        nameLabel.getStyleClass().add("heading-md");
        nameLabel.getStyleClass().add("text-muted");

        titleBox.getChildren().addAll(titleRow, nameLabel);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Bouton Modifier UE
        Button editUeBtn = new Button("Modifier l'UE");
        editUeBtn.getStyleClass().add("btn-secondary");
        editUeBtn.setOnAction(e -> {
            api.views.dialogs.UeDialog dlg = new api.views.dialogs.UeDialog(
                    controller.getContext().getActiveYear() != null ? engine.getSemesters(controller.getContext().getActiveYear().id()) : List.of(),
                    sum.semesterId(),
                    sum
            );
            dlg.showAndWait().ifPresent(draft -> controller.updateUe(ueId, draft, this::refresh));
        });

        // Bouton Supprimer UE
        Button deleteUeBtn = new Button();
        deleteUeBtn.getStyleClass().add("btn-icon");
        FontIcon trash = new FontIcon(Feather.TRASH_2);
        trash.setIconSize(14);
        deleteUeBtn.setGraphic(trash);
        deleteUeBtn.setOnAction(e -> {
            if (ConfirmationDialog.confirm("Supprimer l'UE",
                    "Supprimer l'UE " + sum.code() + " ?",
                    "Toutes les notes et évaluations associées seront supprimées.")) {
                controller.deleteUe(ueId, () -> controller.navigateTo(api.model.NavigationDestination.DASHBOARD));
            }
        });

        // Bouton Ajouter évaluation pour cette UE
        Button addEvalBtn = new Button("Ajouter une évaluation");
        addEvalBtn.getStyleClass().add("btn-primary");
        FontIcon plusIcon = new FontIcon(Feather.PLUS);
        plusIcon.setIconSize(14);
        addEvalBtn.setGraphic(plusIcon);
        addEvalBtn.setOnAction(e -> {
            EvaluationDialog dlg = new EvaluationDialog(engine, sum.semesterId(), null);
            dlg.showAndWait().ifPresent(draft -> controller.createEvaluation(draft, newId -> refresh()));
        });

        header.getChildren().addAll(backBtn, titleBox, spacer, editUeBtn, deleteUeBtn, addEvalBtn);
        content.getChildren().add(header);

        // 2. Carte Résumé KPI de l'UE
        HBox kpiBox = new HBox(20);
        kpiBox.getStyleClass().add("card");
        kpiBox.setAlignment(Pos.CENTER_LEFT);

        VBox avgCol = new VBox(2);
        Label avgTitle = new Label("Moyenne calculée de l'UE");
        avgTitle.getStyleClass().add("stat-label");
        Label avgVal = new Label(Formatters.formatComputedResult(sum.average()));
        avgVal.getStyleClass().add("stat-value");
        avgCol.getChildren().addAll(avgTitle, avgVal);

        VBox progCol = new VBox(2);
        Label progTitle = new Label("Progression");
        progTitle.getStyleClass().add("stat-label");
        EvaluationProgress p = sum.progress();
        Label progVal = new Label(String.format("%d / %d évaluations notées", p.graded(), p.total()));
        progVal.getStyleClass().add("heading-md");
        progCol.getChildren().addAll(progTitle, progVal);

        kpiBox.getChildren().addAll(avgCol, new Separator(javafx.geometry.Orientation.VERTICAL), progCol);
        if (!details.description().isBlank()) {
            VBox descCol = new VBox(2);
            Label descTitle = new Label("Description");
            descTitle.getStyleClass().add("stat-label");
            Label descVal = new Label(details.description());
            descVal.getStyleClass().add("text-muted");
            descVal.setWrapText(true);
            descCol.getChildren().addAll(descTitle, descVal);
            kpiBox.getChildren().addAll(new Separator(javafx.geometry.Orientation.VERTICAL), descCol);
        }
        content.getChildren().add(kpiBox);

        // 3. Tableau des Évaluations de l'UE
        VBox tableSection = new VBox(10);
        Label tableTitle = new Label("Évaluations et Résultats");
        tableTitle.getStyleClass().add("heading-md");
        tableSection.getChildren().add(tableTitle);

        TableView<EvaluationSummary> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        table.setPlaceholder(new Label("Aucune évaluation enregistrée pour cette UE."));

        // Colonne Titre
        TableColumn<EvaluationSummary, String> colTitle = new TableColumn<>("Évaluation");
        colTitle.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().title()));
        colTitle.setPrefWidth(180);

        // Colonne Type
        TableColumn<EvaluationSummary, String> colType = new TableColumn<>("Type");
        colType.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().type().label()));
        colType.setPrefWidth(90);

        // Colonne Date
        TableColumn<EvaluationSummary, String> colDate = new TableColumn<>("Date");
        colDate.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(Formatters.formatDate(data.getValue().date())));
        colDate.setPrefWidth(110);

        // Colonne Statut
        TableColumn<EvaluationSummary, EvaluationStatus> colStatus = new TableColumn<>("Statut");
        colStatus.setCellValueFactory(data -> new javafx.beans.property.SimpleObjectProperty<>(data.getValue().status()));
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(EvaluationStatus item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    setGraphic(StatusBadge.ofEvaluationStatus(item));
                }
            }
        });
        colStatus.setPrefWidth(120);

        // Colonne Note obtenue / Barème
        TableColumn<EvaluationSummary, String> colGrade = new TableColumn<>("Note obtenue");
        colGrade.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
                Formatters.formatGrade(data.getValue().grade(), data.getValue().maxPoints())
        ));
        colGrade.setPrefWidth(120);

        // Colonne Note normalisée (sur 20)
        TableColumn<EvaluationSummary, String> colNorm = new TableColumn<>("Note / 20");
        colNorm.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
                Formatters.formatComputedResult(data.getValue().normalizedScore())
        ));
        colNorm.setPrefWidth(100);

        // Colonne Coefficient
        TableColumn<EvaluationSummary, String> colWeight = new TableColumn<>("Coeff.");
        colWeight.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
                data.getValue().weight().isPresent() ? String.format("%.2f", data.getValue().weight().getAsDouble()) : "1.00"
        ));
        colWeight.setPrefWidth(70);

        // Colonne Actions
        TableColumn<EvaluationSummary, Void> colActions = new TableColumn<>("Actions");
        colActions.setPrefWidth(160);
        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button gradeBtn = new Button();
            private final Button editBtn = new Button();
            private final Button delBtn = new Button();
            private final HBox box = new HBox(6, gradeBtn, editBtn, delBtn);

            {
                box.setAlignment(Pos.CENTER);
                gradeBtn.getStyleClass().add("btn-icon");
                editBtn.getStyleClass().add("btn-icon");
                delBtn.getStyleClass().add("btn-icon");

                FontIcon gIcon = new FontIcon(Feather.EDIT_2);
                gIcon.setIconSize(14);
                gradeBtn.setGraphic(gIcon);
                gradeBtn.setTooltip(new Tooltip("Saisir / Modifier la note"));

                FontIcon eIcon = new FontIcon(Feather.SETTINGS);
                eIcon.setIconSize(14);
                editBtn.setGraphic(eIcon);
                editBtn.setTooltip(new Tooltip("Modifier les paramètres de l'évaluation"));

                FontIcon dIcon = new FontIcon(Feather.TRASH);
                dIcon.setIconSize(14);
                delBtn.setGraphic(dIcon);
                delBtn.setTooltip(new Tooltip("Supprimer l'évaluation"));
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    EvaluationSummary ev = getTableView().getItems().get(getIndex());

                    gradeBtn.setOnAction(e -> {
                        GradeDialog dlg = new GradeDialog(ev);
                        dlg.showAndWait().ifPresent(res -> {
                            if (res instanceof GradeDialog.GradeDialogResult.Save s) {
                                controller.recordGrade(ev.id(), s.entry(), UeDetailsScreen.this::refresh);
                            } else if (res instanceof GradeDialog.GradeDialogResult.Clear) {
                                controller.clearGrade(ev.id(), UeDetailsScreen.this::refresh);
                            }
                        });
                    });

                    editBtn.setOnAction(e -> {
                        EvaluationDialog dlg = new EvaluationDialog(engine, sum.semesterId(), ev);
                        dlg.showAndWait().ifPresent(draft -> controller.updateEvaluation(ev.id(), draft, UeDetailsScreen.this::refresh));
                    });

                    delBtn.setOnAction(e -> {
                        if (ConfirmationDialog.confirm("Supprimer l'évaluation",
                                "Voulez-vous vraiment supprimer \"" + ev.title() + "\" ?",
                                "Cette action supprimera également la note associée.")) {
                            controller.deleteEvaluation(ev.id(), UeDetailsScreen.this::refresh);
                        }
                    });

                    setGraphic(box);
                }
            }
        });

        table.getColumns().addAll(colTitle, colType, colDate, colStatus, colGrade, colNorm, colWeight, colActions);
        table.getItems().setAll(details.evaluations());
        table.setPrefHeight(Math.max(160, details.evaluations().size() * 42 + 45));

        tableSection.getChildren().add(table);
        content.getChildren().add(tableSection);

        // 4. Explications du calcul fournies par le moteur
        if (!details.calculationNotes().isEmpty()) {
            VBox notesBox = new VBox(6);
            notesBox.getStyleClass().add("card");
            Label notesTitle = new Label("Méthode et règles de calcul appliquées par le moteur :");
            notesTitle.getStyleClass().add("heading-sm");
            notesBox.getChildren().add(notesTitle);

            for (String note : details.calculationNotes()) {
                HBox row = new HBox(8);
                row.setAlignment(Pos.CENTER_LEFT);
                FontIcon infoIcon = new FontIcon(Feather.INFO);
                infoIcon.setIconSize(12);
                infoIcon.getStyleClass().add("text-tertiary");
                Label nLabel = new Label(note);
                nLabel.getStyleClass().add("text-muted");
                row.getChildren().addAll(infoIcon, nLabel);
                notesBox.getChildren().add(row);
            }
            content.getChildren().add(notesBox);
        }
    }
}
