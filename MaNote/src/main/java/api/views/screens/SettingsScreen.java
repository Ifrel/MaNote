package api.views.screens;

import api.controllers.AppController;
import api.views.widgets.StatusBadge;
import application.engine.EngineInfo;
import infrastructure.preferences.UserPreferences;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/**
 * Écran 10 — Paramètres généraux & Diagnostic (SettingsScreen).
 */
public class SettingsScreen extends ScrollPane {

    private final AppController controller;
    private final VBox content = new VBox(20);

    public SettingsScreen(AppController controller) {
        this.controller = controller;

        setFitToWidth(true);
        setStyle("-fx-background-color: transparent;");
        content.setPadding(new Insets(24));
        setContent(content);

        buildContent();
    }

    private void buildContent() {
        content.getChildren().clear();

        // 1. En-tête
        VBox titleBox = new VBox(4);
        Label title = new Label("Paramètres de l'Application");
        title.getStyleClass().add("heading-xl");
        Label subtitle = new Label("Préférences visuelles, ergonomie et état de connexion du moteur de calcul.");
        subtitle.getStyleClass().add("text-muted");
        titleBox.getChildren().addAll(title, subtitle);
        content.getChildren().add(titleBox);

        // 2. Section Préférences visuelles
        VBox themeCard = new VBox(12);
        themeCard.getStyleClass().add("card");

        Label themeTitle = new Label("Apparence & Thème");
        themeTitle.getStyleClass().add("heading-md");

        HBox themeRow = new HBox(16);
        themeRow.setAlignment(Pos.CENTER_LEFT);

        Label themeLabel = new Label("Thème de l'interface :");
        themeLabel.getStyleClass().add("heading-sm");

        RadioButton lightRadio = new RadioButton("Clair");
        RadioButton darkRadio = new RadioButton("Sombre");
        ToggleGroup themeGroup = new ToggleGroup();
        lightRadio.setToggleGroup(themeGroup);
        darkRadio.setToggleGroup(themeGroup);

        if (controller.getContext().getThemeMode() == UserPreferences.ThemeMode.DARK) {
            darkRadio.setSelected(true);
        } else {
            lightRadio.setSelected(true);
        }

        themeGroup.selectedToggleProperty().addListener((obs, oldV, newV) -> {
            if (newV == darkRadio) {
                controller.getContext().setThemeMode(UserPreferences.ThemeMode.DARK);
            } else {
                controller.getContext().setThemeMode(UserPreferences.ThemeMode.LIGHT);
            }
        });

        themeRow.getChildren().addAll(themeLabel, lightRadio, darkRadio);
        themeCard.getChildren().addAll(themeTitle, themeRow);
        content.getChildren().add(themeCard);

        // 3. Section Ergonomie & Sécurité
        VBox safetyCard = new VBox(12);
        safetyCard.getStyleClass().add("card");

        Label safetyTitle = new Label("Confirmations d'actions");
        safetyTitle.getStyleClass().add("heading-md");

        CheckBox confirmDeleteBox = new CheckBox("Demander une confirmation avant la suppression d'une évaluation ou d'une note");
        confirmDeleteBox.setSelected(UserPreferences.isConfirmDeleteEnabled());
        confirmDeleteBox.selectedProperty().addListener((obs, oldV, newV) -> {
            UserPreferences.setConfirmDeleteEnabled(newV);
        });

        safetyCard.getChildren().addAll(safetyTitle, confirmDeleteBox);
        content.getChildren().add(safetyCard);

        // 4. Section Moteur Métier Connecté
        VBox engineCard = new VBox(12);
        engineCard.getStyleClass().add("card");

        HBox engineHeader = new HBox(10);
        engineHeader.setAlignment(Pos.CENTER_LEFT);
        Label engineTitle = new Label("Moteur de Calcul Connecté");
        engineTitle.getStyleClass().add("heading-md");

        EngineInfo info = controller.getEngine().info();
        StatusBadge engineBadge = info.demonstration()
                ? StatusBadge.warning("Mode Démonstration")
                : StatusBadge.success("Moteur Métier Actif");

        engineHeader.getChildren().addAll(engineTitle, engineBadge);

        GridPane eGrid = new GridPane();
        eGrid.setHgap(16);
        eGrid.setVgap(8);

        eGrid.add(new Label("Nom du moteur :"), 0, 0);
        Label nameLbl = new Label(info.name());
        nameLbl.getStyleClass().add("heading-sm");
        eGrid.add(nameLbl, 1, 0);

        eGrid.add(new Label("Version :"), 0, 1);
        eGrid.add(new Label(info.version()), 1, 1);

        eGrid.add(new Label("Description :"), 0, 2);
        Label descLbl = new Label(info.description());
        descLbl.getStyleClass().add("text-muted");
        descLbl.setWrapText(true);
        eGrid.add(descLbl, 1, 2);

        VBox noticeBox = new VBox(4);
        noticeBox.setStyle("-fx-padding: 10px; -fx-background-color: -fx-bg-surface-alt; -fx-background-radius: 6px;");
        Label noticeTitle = new Label("Comment brancher votre moteur métier réel ?");
        noticeTitle.getStyleClass().add("heading-sm");
        Label noticeDesc = new Label("Spécifiez simplement l'argument VM : -Dmanote.engine.class=votre.package.VotreGradeEngine ou déclarez votre classe dans META-INF/services/application.engine.GradeEngine. Aucune modification de l'interface n'est requise !");
        noticeDesc.getStyleClass().add("text-muted");
        noticeDesc.setWrapText(true);
        noticeBox.getChildren().addAll(noticeTitle, noticeDesc);

        engineCard.getChildren().addAll(engineHeader, eGrid, noticeBox);
        content.getChildren().add(engineCard);

        // 5. Section Fichier de Configuration YAML du Cursus
        VBox yamlCard = new VBox(12);
        yamlCard.getStyleClass().add("card");

        Label yamlTitle = new Label("Fichier de Configuration du Cursus (YAML)");
        yamlTitle.getStyleClass().add("heading-md");

        Label yamlDesc = new Label("Vous pouvez éditer l'ensemble de votre cursus (Années, Semestres, UE, DS, barèmes, pourcentages) directement dans un fichier texte YAML situé à :\n" +
                System.getProperty("user.home") + "/.manote/cursus.yaml");
        yamlDesc.getStyleClass().add("text-muted");
        yamlDesc.setWrapText(true);

        HBox yamlBtnBox = new HBox(12);
        yamlBtnBox.setAlignment(Pos.CENTER_LEFT);

        Button reloadYamlBtn = new Button("Synchroniser depuis cursus.yaml");
        reloadYamlBtn.getStyleClass().add("btn-primary");
        reloadYamlBtn.setOnAction(e -> controller.syncCurriculumFromYaml(this::buildContent));

        Button exportYamlBtn = new Button("Exporter la base vers cursus.yaml");
        exportYamlBtn.getStyleClass().add("btn-secondary");
        exportYamlBtn.setOnAction(e -> controller.exportCurriculumToYaml(this::buildContent));

        yamlBtnBox.getChildren().addAll(reloadYamlBtn, exportYamlBtn);
        yamlCard.getChildren().addAll(yamlTitle, yamlDesc, yamlBtnBox);
        content.getChildren().add(yamlCard);

        // 6. Section Réinitialisation complète
        VBox resetCard = new VBox(12);
        resetCard.getStyleClass().add("card");

        Label resetTitle = new Label("Réinitialisation de la base de données");
        resetTitle.getStyleClass().add("heading-md");
        Label resetDesc = new Label("Efface toutes les données en base locale (Années, Semestres, UE et Notes) pour repartir d'une installation totalement vierge.");
        resetDesc.getStyleClass().add("text-muted");

        Button resetBtn = new Button("Effacer toutes les données");
        resetBtn.getStyleClass().add("btn-secondary");
        resetBtn.setStyle("-fx-text-fill: -fx-danger-text;");
        resetBtn.setOnAction(e -> {
            if (api.views.dialogs.ConfirmationDialog.confirm(
                    "Réinitialiser la base de données",
                    "Êtes-vous sûr de vouloir tout effacer ?",
                    "Cette action supprimera toutes les années, semestres, UE et notes enregistrées.")) {
                infrastructure.persistence.JpaManager.getInstance().executeInTransaction(em -> {
                    em.createQuery("DELETE FROM Note").executeUpdate();
                    em.createQuery("DELETE FROM UE").executeUpdate();
                    em.createQuery("DELETE FROM Semestre").executeUpdate();
                    em.createQuery("DELETE FROM Annee").executeUpdate();
                });
                controller.loadInitialDataSynchronously();
                controller.getContext().showToast("Base de données entièrement réinitialisée.", api.model.AppContext.ToastType.INFO);
                buildContent();
            }
        });

        resetCard.getChildren().addAll(resetTitle, resetDesc, resetBtn);
        content.getChildren().add(resetCard);

        // 5. Section À propos
        VBox aboutCard = new VBox(8);
        aboutCard.getStyleClass().add("card");

        Label aboutTitle = new Label("À propos de MaNote");
        aboutTitle.getStyleClass().add("heading-md");

        Label aboutText = new Label("MaNote — Application Desktop de Gestion des Notes Universitaires (Master 1 Informatique)\n" +
                "Technologies : Java 25, OpenJFX 25, Ikonli Feather Icons, Inter Font (OFL)\n" +
                "Architecture : MVC Strict & Inversion de dépendance");
        aboutText.getStyleClass().add("text-muted");

        aboutCard.getChildren().addAll(aboutTitle, aboutText);
        content.getChildren().add(aboutCard);
    }
}
