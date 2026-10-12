package infrastructure.demo;

import application.engine.GradeEngine;
import application.engine.error.NotFoundException;
import application.engine.error.ValidationException;
import application.engine.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Tests du contrat GradeEngine via DemoGradeEngine")
class DemoGradeEngineTest {

    private GradeEngine engine;

    @BeforeEach
    void setUp() {
        engine = new DemoGradeEngine(LocalDate.of(2026, 11, 15));
    }

    @Test
    @DisplayName("Le moteur déclare être un moteur de démonstration")
    void testEngineInfo() {
        var info = engine.info();
        assertNotNull(info);
        assertTrue(info.demonstration(), "Doit être explicitement marqué comme démonstration");
        assertFalse(info.name().isBlank());
    }

    @Test
    @DisplayName("Les années académiques et semestres sont chargés avec cohérence")
    void testAcademicStructure() {
        List<AcademicYearSummary> years = engine.getAcademicYears();
        assertFalse(years.isEmpty(), "Au moins une année doit être disponible");

        var currentYear = years.stream().filter(AcademicYearSummary::current).findFirst();
        assertTrue(currentYear.isPresent(), "Une année courante doit exister");

        List<SemesterSummary> sems = engine.getSemesters(currentYear.get().id());
        assertFalse(sems.isEmpty(), "Le semestre doit contenir des entrées");

        var s1 = sems.get(0);
        List<UeSummary> ues = engine.getUes(s1.id());
        assertFalse(ues.isEmpty(), "Des UE doivent être rattachées au semestre");

        var ueDetails = engine.getUe(ues.get(0).id());
        assertNotNull(ueDetails);
        assertFalse(ueDetails.evaluations().isEmpty());
    }

    @Test
    @DisplayName("Invariant : une note absente ou en attente n'est jamais assimilée à zéro")
    void testMissingGradeIsNotZero() {
        var evals = engine.getEvaluations(EvaluationFilter.all());
        var pendingEvals = evals.stream()
                .filter(e -> e.status() == EvaluationStatus.PLANNED || e.status() == EvaluationStatus.AWAITING_GRADE)
                .toList();

        assertFalse(pendingEvals.isEmpty(), "Il doit y avoir des évaluations en attente dans le jeu de test");

        for (var e : pendingEvals) {
            assertTrue(e.grade().isEmpty(),
                    "Une évaluation planifiée ou en attente ne doit pas avoir d'objet GradeEntry");
            assertFalse(e.normalizedScore().hasValue(),
                    "Une évaluation sans note ne doit jamais avoir une valeur numérique fictive ou zéro");
        }
    }

    @Test
    @DisplayName("Invariant : grade présent ssi status == GRADED")
    void testGradedInvariant() {
        var evals = engine.getEvaluations(EvaluationFilter.all());
        for (var e : evals) {
            if (e.status() == EvaluationStatus.GRADED) {
                assertTrue(e.grade().isPresent(), "Statut GRADED exige une note présente");
            } else {
                assertTrue(e.grade().isEmpty(), "Statut non GRADED interdit une note présente");
            }
        }
    }

    @Test
    @DisplayName("Enregistrement, modification et retrait d'une note")
    void testGradeRecordAndClear() {
        var ues = engine.getUes(engine.getSemesters(engine.getAcademicYears().get(0).id()).get(0).id());
        var ue = ues.get(0);

        // Création d'une évaluation planifiée
        EvaluationDraft draft = new EvaluationDraft(
                "Contrôle Surprise",
                ue.id(),
                "CC",
                Optional.of(LocalDate.of(2026, 11, 20)),
                EvaluationStatus.PLANNED,
                20.0,
                OptionalDouble.of(1.5),
                "Test d'intégration"
        );
        EvaluationId evalId = engine.createEvaluation(draft);
        assertNotNull(evalId);

        var detailsBefore = engine.getEvaluation(evalId);
        assertEquals(EvaluationStatus.PLANNED, detailsBefore.summary().status());
        assertTrue(detailsBefore.summary().grade().isEmpty());

        // Enregistrement de la note
        engine.recordGrade(evalId, GradeEntry.score(16.5, 20.0));

        var detailsGraded = engine.getEvaluation(evalId);
        assertEquals(EvaluationStatus.GRADED, detailsGraded.summary().status());
        assertTrue(detailsGraded.summary().grade().isPresent());
        var score = (GradeEntry.Score) detailsGraded.summary().grade().get();
        assertEquals(16.5, score.points());

        // Retrait de la note
        engine.clearGrade(evalId);

        var detailsCleared = engine.getEvaluation(evalId);
        assertEquals(EvaluationStatus.AWAITING_GRADE, detailsCleared.summary().status());
        assertTrue(detailsCleared.summary().grade().isEmpty());

        // Suppression de l'évaluation
        engine.deleteEvaluation(evalId);
        assertThrows(NotFoundException.class, () -> engine.getEvaluation(evalId));
    }

    @Test
    @DisplayName("Validation d'une note supérieure au barème lève une ValidationException")
    void testGradeAboveMaxPointsThrows() {
        var ues = engine.getUes(engine.getSemesters(engine.getAcademicYears().get(0).id()).get(0).id());
        var evalId = engine.createEvaluation(new EvaluationDraft(
                "Quiz Test", ues.get(0).id(), "QUIZ", Optional.empty(),
                EvaluationStatus.PLANNED, 10.0, OptionalDouble.empty(), ""
        ));

        assertThrows(ValidationException.class, () -> {
            engine.recordGrade(evalId, GradeEntry.score(15.0, 10.0));
        });
    }

    @Test
    @DisplayName("La simulation produit des résultats marqués explicitement comme SIMULATED")
    void testSimulationProducesSimulatedNature() {
        var s1 = engine.getSemesters(engine.getAcademicYears().get(0).id()).get(0);
        var ues = engine.getUes(s1.id());
        var evals = engine.getEvaluations(new EvaluationFilter(
                Optional.of(s1.id()), Optional.empty(), Optional.empty(),
                java.util.Set.of(EvaluationStatus.PLANNED), Optional.empty(), Optional.empty(), ""
        ));

        assertFalse(evals.isEmpty());
        var targetEval = evals.get(0);

        var simResult = engine.simulate(new SimulationRequest(
                s1.id(),
                java.util.Map.of(targetEval.id(), GradeEntry.score(18.0, 20.0))
        ));

        assertNotNull(simResult);
        assertTrue(simResult.semesterAverage().hasValue());
        var val = simResult.semesterAverage().asValue().orElseThrow();
        assertEquals(ResultNature.SIMULATED, val.nature(), "Le résultat d'une simulation doit porter la nature SIMULATED");
    }
}
