package application.engine;

import application.engine.config.ConfigValue;
import application.engine.config.ConfigurationScope;
import application.engine.model.*;
import infrastructure.persistence.JpaManager;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Tests du moteur persistant JpaGradeEngine (Base relationnelle)")
class JpaGradeEngineTest {

    private static JpaGradeEngine engine;

    @BeforeAll
    static void setUp() {
        // Initialiser en base mémoire isolée pour la suite de tests
        JpaManager.resetForTesting("test_engine_db");
        infrastructure.persistence.DatabaseInitializer.seedDemoDataForTesting(JpaManager.getInstance());
        engine = new JpaGradeEngine(JpaManager.getInstance());
    }

    @AfterAll
    static void tearDown() {
        JpaManager.getInstance().close();
    }

    @Test
    @DisplayName("Le moteur fournit ses métadonnées et ses capacités persistantes")
    void testEngineInfoAndCapabilities() {
        EngineInfo info = engine.info();
        assertNotNull(info);
        assertFalse(info.demonstration(), "Le moteur JPA ne doit pas être en mode démonstration");

        EngineCapabilities caps = engine.capabilities();
        assertTrue(caps.structureEditing());
        assertTrue(caps.simulation());
        assertTrue(caps.averageTimeline());
    }

    @Test
    @DisplayName("Données réelles initiales : Années, Semestres et UEs disponibles")
    void testInitialDataLoaded() {
        List<AcademicYearSummary> years = engine.getAcademicYears();
        assertFalse(years.isEmpty(), "Au moins une année universitaire doit être présente");

        AcademicYearSummary activeYear = years.get(0);
        List<SemesterSummary> semesters = engine.getSemesters(activeYear.id());
        assertEquals(2, semesters.size(), "Le cursus M1 doit contenir 2 semestres (S7 et S8)");

        SemesterSummary s7 = semesters.get(0);
        List<UeSummary> ues = engine.getUes(s7.id());
        assertFalse(ues.isEmpty(), "Le semestre 7 doit contenir des UE");
        assertTrue(ues.stream().anyMatch(u -> "INFO-M1-CL".equals(u.code())));
    }

    @Test
    @DisplayName("Une note non notée n'est JAMAIS assimilée à zéro")
    void testMissingGradeIsNotZero() {
        List<EvaluationSummary> evals = engine.getEvaluations(EvaluationFilter.all().withText("Application Desktop"));
        assertFalse(evals.isEmpty());
        EvaluationSummary project = evals.get(0);

        assertEquals(EvaluationStatus.AWAITING_GRADE, project.status());
        assertEquals(Optional.empty(), project.grade());
        assertTrue(project.normalizedScore() instanceof ComputedResult.NotComputable);
    }

    @Test
    @DisplayName("Calcul de la moyenne d'une UE et du semestre")
    void testComputations() {
        List<SemesterSummary> sems = engine.getSemesters(engine.getAcademicYears().get(0).id());
        SemesterSummary s7 = sems.get(0);

        assertTrue(s7.average() instanceof ComputedResult.Value);
        ComputedResult.Value avg = (ComputedResult.Value) s7.average();
        assertTrue(avg.value() > 0 && avg.value() <= 20.0);

        List<TimelinePoint> timeline = engine.getAverageTimeline(s7.id());
        assertFalse(timeline.isEmpty(), "La chronologie des moyennes doit contenir des points");
    }

    @Test
    @DisplayName("CRUD Évaluation : création, saisie de note, suppression")
    void testEvaluationLifecycle() {
        List<UeSummary> ues = engine.getUes(engine.getSemesters(engine.getAcademicYears().get(0).id()).get(0).id());
        UeId ueId = ues.get(0).id();

        // 1. Création
        EvaluationDraft draft = new EvaluationDraft(
                "Test Unitaire Automatisé",
                ueId,
                "TP",
                Optional.of(LocalDate.now()),
                EvaluationStatus.PLANNED,
                20.0,
                OptionalDouble.of(1.0),
                "Description test"
        );
        EvaluationId newId = engine.createEvaluation(draft);
        assertNotNull(newId);

        // Vérification
        EvaluationDetails details = engine.getEvaluation(newId);
        assertEquals("Test Unitaire Automatisé", details.summary().title());
        assertEquals(EvaluationStatus.PLANNED, details.summary().status());

        // 2. Saisie d'une note
        engine.recordGrade(newId, new GradeEntry.Score(17.5, 20.0));
        EvaluationDetails updated = engine.getEvaluation(newId);
        assertEquals(EvaluationStatus.GRADED, updated.summary().status());
        assertTrue(updated.summary().grade().isPresent());

        // 3. Suppression
        engine.deleteEvaluation(newId);
        assertThrows(Exception.class, () -> engine.getEvaluation(newId));
    }

    @Test
    @DisplayName("Simulation de notes futures")
    void testSimulation() {
        SemesterSummary s7 = engine.getSemesters(engine.getAcademicYears().get(0).id()).get(0);
        List<EvaluationSummary> evals = engine.getEvaluations(EvaluationFilter.all().withSemester(s7.id()).withStatus(EvaluationStatus.PLANNED));

        if (!evals.isEmpty()) {
            EvaluationId target = evals.get(0).id();
            SimulationRequest req = new SimulationRequest(s7.id(), Map.of(target, new GradeEntry.Score(19.0, 20.0)));
            SimulationResult res = engine.simulate(req);

            assertNotNull(res);
            assertTrue(res.semesterAverage() instanceof ComputedResult.Value);
        }
    }

    @Test
    @DisplayName("Configuration dynamique du moteur persistant")
    void testConfiguration() {
        ConfigurationScope globalScope = new ConfigurationScope.Global();
        var config = engine.getConfiguration(globalScope);
        assertNotNull(config);
        assertFalse(config.sections().isEmpty());

        engine.updateConfiguration(globalScope, Map.of(
                "validation.threshold", ConfigValue.number(11.0)
        ));
        engine.resetConfiguration(globalScope);
    }

    @Test
    @DisplayName("Édition structurelle : Année, Semestre, UE")
    void testStructuralEditing() {
        // 1. Création d'une nouvelle année académique
        AcademicYearDraft yearDraft = new AcademicYearDraft(
                "Master 2 Informatique",
                "M2",
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2027, 6, 30)
        );
        AcademicYearId yearId = engine.createAcademicYear(yearDraft);
        assertNotNull(yearId);

        // Mise à jour de l'année
        engine.updateAcademicYear(yearId, new AcademicYearDraft(
                "Master 2 Informatique (MAST-2)",
                "M2",
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2027, 6, 30)
        ));

        // 2. Création d'un semestre
        SemesterDraft semDraft = new SemesterDraft(
                yearId,
                9,
                "Semestre 9",
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2027, 1, 15)
        );
        SemesterId semId = engine.createSemester(semDraft);
        assertNotNull(semId);

        // Mise à jour du semestre
        engine.updateSemester(semId, new SemesterDraft(
                yearId,
                9,
                "Semestre 9 - M2",
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2027, 1, 15)
        ));

        // 3. Création d'une UE
        UeId ueId = engine.createUe(new UeDraft(semId, "M2-DEVOPS", "DevOps & Cloud Computing", 6, "UE Cloud"));
        assertNotNull(ueId);

        UeDetails details = engine.getUe(ueId);
        assertEquals("DevOps & Cloud Computing", details.summary().name());
        assertEquals(6, details.summary().credits());

        // Mise à jour de l'UE
        engine.updateUe(ueId, new UeDraft(semId, "M2-CLOUD", "Cloud & Microservices", 6, "UE Cloud et conteneurs"));
        assertEquals("Cloud & Microservices", engine.getUe(ueId).summary().name());

        // Suppression de l'UE
        engine.deleteUe(ueId);
        assertThrows(Exception.class, () -> engine.getUe(ueId));
    }
}
