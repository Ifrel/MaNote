package api.views.app;

import api.controllers.AppController;
import infrastructure.engine.EngineLoader;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.text.Font;
import javafx.stage.Stage;

import java.io.InputStream;

/**
 * Application JavaFX principale MaNote.
 */
public class MaNoteApp extends Application {

    @Override
    public void init() {
        loadEmbeddedFonts();
    }

    private void loadEmbeddedFonts() {
        String[] fontFiles = {
                "/fonts/Inter-Regular.ttf",
                "/fonts/Inter-Medium.ttf",
                "/fonts/Inter-SemiBold.ttf",
                "/fonts/Inter-Bold.ttf"
        };
        for (String file : fontFiles) {
            try (InputStream is = getClass().getResourceAsStream(file)) {
                if (is != null) {
                    Font.loadFont(is, 12);
                }
            } catch (Exception ignored) {
            }
        }
    }

    private AppController controller;

    @Override
    public void start(Stage primaryStage) {
        controller = new AppController(EngineLoader.getEngine(), new api.model.AppContext());

        // Chargement initial des données réelles avant la construction de l'interface
        controller.loadInitialDataSynchronously();

        AppShell shell = new AppShell(controller);

        Scene scene = new Scene(shell, 1200, 780);

        // Appliquer le thème initial
        shell.applyTheme(controller.getContext().getThemeMode());

        // Réaction aux changements de thème
        controller.getContext().themeModeProperty().addListener((obs, oldV, newV) -> {
            shell.applyTheme(newV);
        });

        primaryStage.setTitle("MaNote — Gestion des Notes Universitaires");
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(960);
        primaryStage.setMinHeight(640);
        primaryStage.show();
    }

    @Override
    public void stop() {
        if (controller != null) {
            controller.shutdown();
        }
    }
}
