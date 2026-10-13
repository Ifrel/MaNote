package api.views.app;

import infrastructure.preferences.UserPreferences;
import javafx.scene.control.DialogPane;

import java.util.Objects;

/**
 * Point unique pour obtenir les feuilles de style du thème courant.
 * Les dialogues l'utilisent pour suivre le thème (clair ou sombre) au lieu d'être figés en clair.
 */
public final class Theme {

    private Theme() {
    }

    public static String[] stylesheets(UserPreferences.ThemeMode mode) {
        String vars = mode == UserPreferences.ThemeMode.DARK
                ? "/styles/variables-dark.css"
                : "/styles/variables-light.css";
        return new String[]{url(vars), url("/styles/base.css"), url("/styles/charts.css")};
    }

    /**
     * Applique le thème courant à un dialogue.
     */
    public static void styleDialog(DialogPane pane) {
        pane.getStylesheets().setAll(stylesheets(UserPreferences.getTheme()));
    }

    private static String url(String path) {
        return Objects.requireNonNull(Theme.class.getResource(path), path).toExternalForm();
    }
}
