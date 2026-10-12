package infrastructure.engine;

import application.engine.GradeEngine;
import application.engine.JpaGradeEngine;
import infrastructure.demo.DemoGradeEngine;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Tests d'injection et de chargement du moteur (EngineLoader)")
class EngineLoaderTest {

    @BeforeEach
    @AfterEach
    void resetEngine() {
        System.clearProperty(EngineLoader.ENGINE_PROPERTY);
        EngineLoader.setEngine(null);
    }

    @Test
    @DisplayName("Par défaut, EngineLoader démarre sur JpaGradeEngine connecté aux données réelles")
    void testDefaultEngine() {
        GradeEngine engine = EngineLoader.getEngine();
        assertNotNull(engine);
        assertInstanceOf(JpaGradeEngine.class, engine);
        assertFalse(engine.info().demonstration(), "Le moteur par défaut est le moteur persistant réel");
    }

    @Test
    @DisplayName("Le paramètre 'demo' charge explicitement le DemoGradeEngine")
    void testExplicitDemoEngine() {
        System.setProperty(EngineLoader.ENGINE_PROPERTY, "demo");
        GradeEngine engine = EngineLoader.getEngine();
        assertNotNull(engine);
        assertInstanceOf(DemoGradeEngine.class, engine);
        assertTrue(engine.info().demonstration());
    }

    @Test
    @DisplayName("Possibilité d'injecter manuellement une implémentation de test")
    void testManualInjection() {
        DemoGradeEngine custom = new DemoGradeEngine();
        EngineLoader.setEngine(custom);
        assertSame(custom, EngineLoader.getEngine());
    }
}
