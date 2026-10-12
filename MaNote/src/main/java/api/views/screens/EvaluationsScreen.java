package api.views.screens;

import api.controllers.AppController;
import api.views.dialogs.ConfirmationDialog;
import api.views.dialogs.EvaluationDialog;
import api.views.dialogs.GradeDialog;
import api.views.format.Formatters;
import api.views.widgets.StatusBadge;
import application.engine.GradeEngine;
import application.engine.model.*;
import javafx.collections.FXCollections;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.List;

/**
 * Écran 4 — Gestion de toutes les évaluations (EvaluationsScreen).
 * Permet de filtrer, rechercher, trier et administrer les évaluations.
 */
public class EvaluationsScreen extends VBox {

    private final AppController controller;
    private final TableView<EvaluationSummary> table = new TableView<>();
    private final TextField searchField = new TextField();
    private final ComboBox<UeSummary> ueFilterCombo = new ComboBox<>();
    private final ComboBox<EvaluationType> typeFilterCombo = new ComboBox<>();
    private final ComboBox<String> statusFilterCombo = new ComboBox<>();
    private final Label countLabel = new Label();

    private final javafx.collections.ObservableList<EvaluationSummary> masterData = FXCollections.observableArrayList();

    public EvaluationsScreen(AppController controller) {
        this.controller = controller;

        setSpacing(16);
        setPadding(new Insets(24));
        setStyle("-fx-background-color: transparent;");

        buildTopBar();
        buildFilterBar();
        buildTable();

        controller.getContext().activeSemesterProperty().addListener((obs, oldV, newV) -> refresh());
        refresh();
    }

    private void buildTopBar() {
        HBox topBox = new HBox(16);
        topBox.setAlignment(Pos.CENTER_LEFT);

        VBox titleBox = new VBox(4);
        Label title = new Label("Toutes les Évaluations");
        title.getStyleClass().add("heading-xl");
        Label subtitle = new Label("Recherchez, filtrez et gérez vos notes et évaluations du semestre.");
        subtitle.getStyleClass().add("text-muted");
        titleBox.getChildren().addAll(title, subtitle);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button addBtn = new Button("Nouvelle évaluation");
        addBtn.getStyleClass().add("btn-primary");
        FontIcon icon = new FontIcon(Feather.PLUS);
        icon.setIconSize(14);
        addBtn.setGraphic(icon);
        addBtn.setOnAction(e -> {
            SemesterSummary sem = controller.getContext().getActiveSemester();
            EvaluationDialog dlg = new EvaluationDialog(controller.getEngine(), sem != null ? sem.id() : null, null);
            dlg.showAndWait().ifPresent(draft -> controller.createEvaluation(draft, id -> refresh()));
        });

        topBox.getChildren().addAll(titleBox, spacer, addBtn);
        getChildren().add(topBox);
    }

    private void buildFilterBar() {
        HBox filterBar = new HBox(12);
        filterBar.setAlignment(Pos.CENTER_LEFT);
        filterBar.getStyleClass().add("card");
        filterBar.setStyle("-fx-padding: 12px 16px;");

        // Recherche texte
        searchField.setPromptText("Rechercher (titre, UE...)");
        searchField.setPrefWidth(200);

        // Filtre UE
        ueFilterCombo.setPromptText("Toutes les UE");
        ueFilterCombo.setPrefWidth(160);
        ueFilterCombo.setConverter(new StringConverter<>() {
            @Override
            public String toString(UeSummary ue) {
                return ue != null ? ue.code() : "Toutes les UE";
            }

            @Override
            public UeSummary fromString(String string) {
                return null;
            }
        });

        // Filtre Type
        typeFilterCombo.setPromptText("Tous les types");
        typeFilterCombo.setPrefWidth(140);
        typeFilterCombo.setConverter(new StringConverter<>() {
            @Override
            public String toString(EvaluationType t) {
                return t != null ? t.label() : "Tous les types";
            }

            @Override
            public EvaluationType fromString(String string) {
                return null;
            }
        });

        // Filtre Statut
        statusFilterCombo.setPromptText("Tous les statuts");
        statusFilterCombo.getItems().setAll("Tous les statuts", "Planifiée", "Note en attente", "Notée", "Annulée");
        statusFilterCombo.setValue("Tous les statuts");
        statusFilterCombo.setPrefWidth(140);

        Button resetBtn = new Button("Réinitialiser");
        resetBtn.getStyleClass().add("btn-ghost");
        resetBtn.setOnAction(e -> {
            searchField.clear();
            ueFilterCombo.setValue(null);
            typeFilterCombo.setValue(null);
            statusFilterCombo.setValue("Tous les statuts");
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        countLabel.getStyleClass().add("text-tertiary");

        filterBar.getChildren().addAll(
                new FontIcon(Feather.SEARCH), searchField,
                ueFilterCombo, typeFilterCombo, statusFilterCombo,
                resetBtn, spacer, countLabel
        );
        getChildren().add(filterBar);
    }

    @SuppressWarnings("unchecked")
    private void buildTable() {
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        VBox.setVgrow(table, Priority.ALWAYS);
        table.setPlaceholder(new Label("Aucune évaluation ne correspond aux critères de recherche."));

        // Colonne Titre
        TableColumn<EvaluationSummary, String> colTitle = new TableColumn<>("Intitulé");
        colTitle.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue().title()));
        colTitle.setPrefWidth(200);

        // Colonne UE
        TableColumn<EvaluationSummary, String> colUe = new TableColumn<>("UE");
        colUe.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue().ue().code() + " — " + d.getValue().ue().name()));
        colUe.setPrefWidth(160);

        // Colonne Type
        TableColumn<EvaluationSummary, String> colType = new TableColumn<>("Type");
        colType.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue().type().label()));
        colType.setPrefWidth(100);

        // Colonne Date
        TableColumn<EvaluationSummary, String> colDate = new TableColumn<>("Date");
        colDate.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(Formatters.formatDate(d.getValue().date())));
        colDate.setPrefWidth(110);

        // Colonne Statut
        TableColumn<EvaluationSummary, EvaluationStatus> colStatus = new TableColumn<>("Statut");
        colStatus.setCellValueFactory(d -> new javafx.beans.property.SimpleObjectProperty<>(d.getValue().status()));
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(EvaluationStatus item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) setGraphic(null);
                else setGraphic(StatusBadge.ofEvaluationStatus(item));
            }
        });
        colStatus.setPrefWidth(120);

        // Colonne Note obtenue
        TableColumn<EvaluationSummary, String> colGrade = new TableColumn<>("Note obtenue");
        colGrade.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(
                Formatters.formatGrade(d.getValue().grade(), d.getValue().maxPoints())
        ));
        colGrade.setPrefWidth(120);

        // Colonne Note / 20
        TableColumn<EvaluationSummary, String> colScore = new TableColumn<>("Note / 20");
        colScore.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(
                Formatters.formatComputedResult(d.getValue().normalizedScore())
        ));
        colScore.setPrefWidth(100);

        // Colonne Actions
        TableColumn<EvaluationSummary, Void> colActions = new TableColumn<>("Actions");
        colActions.setPrefWidth(150);
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

                FontIcon g = new FontIcon(Feather.EDIT_2);
                g.setIconSize(14);
                gradeBtn.setGraphic(g);
                gradeBtn.setTooltip(new Tooltip("Saisir / Modifier la note"));

                FontIcon e = new FontIcon(Feather.SETTINGS);
                e.setIconSize(14);
                editBtn.setGraphic(e);
                editBtn.setTooltip(new Tooltip("Modifier"));

                FontIcon d = new FontIcon(Feather.TRASH);
                d.setIconSize(14);
                delBtn.setGraphic(d);
                delBtn.setTooltip(new Tooltip("Supprimer"));
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    EvaluationSummary ev = getTableView().getItems().get(getIndex());
                    gradeBtn.setOnAction(evt -> {
                        GradeDialog dlg = new GradeDialog(ev);
                        dlg.showAndWait().ifPresent(res -> {
                            if (res instanceof GradeDialog.GradeDialogResult.Save s) {
                                controller.recordGrade(ev.id(), s.entry(), EvaluationsScreen.this::refresh);
                            } else if (res instanceof GradeDialog.GradeDialogResult.Clear) {
                                controller.clearGrade(ev.id(), EvaluationsScreen.this::refresh);
                            }
                        });
                    });

                    editBtn.setOnAction(evt -> {
                        SemesterSummary sem = controller.getContext().getActiveSemester();
                        EvaluationDialog dlg = new EvaluationDialog(controller.getEngine(), sem != null ? sem.id() : null, ev);
                        dlg.showAndWait().ifPresent(draft -> controller.updateEvaluation(ev.id(), draft, EvaluationsScreen.this::refresh));
                    });

                    delBtn.setOnAction(evt -> {
                        if (ConfirmationDialog.confirm("Supprimer l'évaluation", "Supprimer \"" + ev.title() + "\" ?", "Cette action est irréversible.")) {
                            controller.deleteEvaluation(ev.id(), EvaluationsScreen.this::refresh);
                        }
                    });

                    setGraphic(box);
                }
            }
        });

        table.getColumns().addAll(colTitle, colUe, colType, colDate, colStatus, colGrade, colScore, colActions);

        // Configuration du filtrage réactif
        FilteredList<EvaluationSummary> filteredData = new FilteredList<>(masterData, p -> true);

        Runnable filterUpdater = () -> {
            String query = searchField.getText() != null ? searchField.getText().toLowerCase().trim() : "";
            UeSummary selectedUe = ueFilterCombo.getValue();
            EvaluationType selectedType = typeFilterCombo.getValue();
            String selectedStatus = statusFilterCombo.getValue();

            filteredData.setPredicate(e -> {
                // Recherche texte
                if (!query.isEmpty()) {
                    boolean match = e.title().toLowerCase().contains(query) ||
                            e.ue().code().toLowerCase().contains(query) ||
                            e.ue().name().toLowerCase().contains(query);
                    if (!match) return false;
                }
                // Filtre UE
                if (selectedUe != null && !e.ue().id().equals(selectedUe.id())) {
                    return false;
                }
                // Filtre Type
                if (selectedType != null && !e.type().code().equals(selectedType.code())) {
                    return false;
                }
                // Filtre Statut
                if (selectedStatus != null && !selectedStatus.equals("Tous les statuts")) {
                    String sLabel = Formatters.formatEvaluationStatus(e.status());
                    if (!sLabel.equalsIgnoreCase(selectedStatus)) return false;
                }
                return true;
            });
            countLabel.setText(filteredData.size() + " évaluation(s)");
        };

        searchField.textProperty().addListener((obs, oldV, newV) -> filterUpdater.run());
        ueFilterCombo.valueProperty().addListener((obs, oldV, newV) -> filterUpdater.run());
        typeFilterCombo.valueProperty().addListener((obs, oldV, newV) -> filterUpdater.run());
        statusFilterCombo.valueProperty().addListener((obs, oldV, newV) -> filterUpdater.run());

        SortedList<EvaluationSummary> sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(table.comparatorProperty());
        table.setItems(sortedData);

        getChildren().add(table);
    }

    public void refresh() {
        SemesterSummary sem = controller.getContext().getActiveSemester();
        GradeEngine engine = controller.getEngine();

        if (sem == null) {
            masterData.clear();
            return;
        }

        // Charger les filtres UE et Type
        List<UeSummary> ues = engine.getUes(sem.id());
        ueFilterCombo.getItems().setAll((UeSummary) null);
        ueFilterCombo.getItems().addAll(ues);

        List<EvaluationType> types = engine.getEvaluationTypes();
        typeFilterCombo.getItems().setAll((EvaluationType) null);
        typeFilterCombo.getItems().addAll(types);

        // Charger les évaluations
        List<EvaluationSummary> list = engine.getEvaluations(EvaluationFilter.all().withSemester(sem.id()));
        masterData.setAll(list);
        countLabel.setText(masterData.size() + " évaluation(s)");
    }
}
