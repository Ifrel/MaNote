package infrastructure.engine;

import application.engine.GradeEngine;
import infrastructure.demo.DemoGradeEngine;

import java.util.Iterator;
import java.util.ServiceLoader;

/**
 * Point de chargement et d'injection du moteur de calcul {@link GradeEngine}.
 * <p>
 * L'application utilise cette classe pour obtenir l'instance unique du moteur.
 * <h3>Mécanisme de sélection du moteur :</h3>
 * <ol>
 *     <li>Propriété système Java {@code -Dmanote.engine.class=com.monpackage.MonMoteur} :
 *         instancie dynamiquement la classe spécifiée via son constructeur public sans argument ;</li>
 *     <li>Recherche via le mécanisme standard {@link ServiceLoader} pour l'interface {@link GradeEngine} ;</li>
 *     <li>Repli par défaut sur {@link DemoGradeEngine}.</li>
 * </ol>
 * <p>
 * Ce mécanisme permet à l'utilisateur de brancher son moteur métier définitif
 * <strong>sans modifier une seule ligne du code de présentation ou des contrôleurs</strong>.
 */
public final class EngineLoader {

    public static final String ENGINE_PROPERTY = "manote.engine.class";

    private static volatile GradeEngine instance;

    private EngineLoader() {
    }

    /**
     * Retourne l'instance partagée du moteur de calcul.
     *
     * @return instance configurée du {@link GradeEngine}
     */
    public static GradeEngine getEngine() {
        if (instance == null) {
            synchronized (EngineLoader.class) {
                if (instance == null) {
                    instance = loadEngine();
                }
            }
        }
        return instance;
    }

    /**
     * Permet d'injecter explicitement une implémentation (particulièrement utile pour les tests d'intégration).
     *
     * @param engine moteur à injecter, ou {@code null} pour réinitialiser
     */
    public static void setEngine(GradeEngine engine) {
        synchronized (EngineLoader.class) {
            instance = engine;
        }
    }

    private static GradeEngine loadEngine() {
        // 1. Vérification de la propriété système / CLI
        String customClassName = System.getProperty(ENGINE_PROPERTY);
        if (customClassName != null && !customClassName.isBlank()) {
            if ("demo".equalsIgnoreCase(customClassName.trim())) {
                System.out.println("[EngineLoader] Mode démo demandé explicitement -> Démarrage avec DemoGradeEngine.");
                return new DemoGradeEngine();
            }
            try {
                Class<?> clazz = Class.forName(customClassName.trim());
                if (GradeEngine.class.isAssignableFrom(clazz)) {
                    Object obj = clazz.getDeclaredConstructor().newInstance();
                    System.out.println("[EngineLoader] Moteur chargé via propriété système : " + customClassName);
                    return (GradeEngine) obj;
                } else {
                    System.err.println("[EngineLoader] La classe " + customClassName + " n'implémente pas GradeEngine.");
                }
            } catch (Exception e) {
                System.err.println("[EngineLoader] Impossible de charger le moteur personnalisé : " + e.getMessage());
                e.printStackTrace();
            }
        }

        // 2. Recherche via ServiceLoader
        try {
            ServiceLoader<GradeEngine> loader = ServiceLoader.load(GradeEngine.class);
            Iterator<GradeEngine> iterator = loader.iterator();
            if (iterator.hasNext()) {
                GradeEngine loaded = iterator.next();
                System.out.println("[EngineLoader] Moteur découvert via ServiceLoader : " + loaded.info().name());
                return loaded;
            }
        } catch (Exception e) {
            System.err.println("[EngineLoader] Erreur lors de la recherche ServiceLoader : " + e.getMessage());
        }

        // 3. Moteur par défaut : Moteur Réel Persistant JPA / H2
        try {
            System.out.println("[EngineLoader] Démarrage du moteur réel persistant (JpaGradeEngine)...");
            return new application.engine.JpaGradeEngine();
        } catch (Exception e) {
            System.err.println("[EngineLoader] Erreur d'initialisation JPA (" + e.getMessage() + "), repli de sécurité sur DemoGradeEngine.");
            e.printStackTrace();
            return new DemoGradeEngine();
        }
    }
}
