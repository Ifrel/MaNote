import api.views.app.MaNoteApp;
import javafx.application.Application;

/**
 * Point d'entrée principal de l'application MaNote.
 * <p>
 * Lance l'application de bureau moderne JavaFX {@link MaNoteApp}.
 */
public class MaNote {

    public static void main(String[] args) {
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (arg.startsWith("--engine=")) {
                System.setProperty(infrastructure.engine.EngineLoader.ENGINE_PROPERTY, arg.substring("--engine=".length()));
            } else if ("--engine".equals(arg) && i + 1 < args.length) {
                System.setProperty(infrastructure.engine.EngineLoader.ENGINE_PROPERTY, args[++i]);
            } else if ("--swing".equals(arg)) {
                // Mode de compatibilité Swing hérité
                javax.swing.SwingUtilities.invokeLater(() -> {
                    api.views.app.App app = new api.views.app.App("MaNote — Mode Swing");
                    app.setSize(800, 600);
                    app.setLocationRelativeTo(null);
                    app.setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
                    app.setVisible(true);
                });
                return;
            }
        }
        Application.launch(MaNoteApp.class, args);
    }
}
