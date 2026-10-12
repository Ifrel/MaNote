package api.views.format;

import application.engine.model.ComputedResult;
import application.engine.model.EvaluationStatus;
import application.engine.model.GradeEntry;
import application.engine.model.ResultNature;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Tests des formateurs d'interface (Formatters)")
class FormattersTest {

    @Test
    @DisplayName("Une note absente (Optional.empty) est formatée avec un tiret et jamais avec 0")
    void testFormatMissingGrade() {
        String result = Formatters.formatGrade(Optional.empty(), 20.0);
        assertEquals("—", result);
        assertFalse(result.contains("0"), "Une note absente ne doit jamais contenir la valeur zéro");
    }

    @Test
    @DisplayName("Une note chiffrée est correctement formatée")
    void testFormatScoreGrade() {
        String result = Formatters.formatGrade(Optional.of(GradeEntry.score(14.5, 20.0)), 20.0);
        assertEquals("14,50 / 20", result);
    }

    @Test
    @DisplayName("Une absence justifiée est explicitement identifiée")
    void testFormatAbsenceJustified() {
        String result = Formatters.formatGrade(Optional.of(GradeEntry.absence(true)), 20.0);
        assertTrue(result.contains("ABJ") || result.contains("justifiée"));
    }

    @Test
    @DisplayName("Un résultat calculé non disponible fournit sa raison lisible")
    void testFormatNotComputable() {
        ComputedResult nc = ComputedResult.notComputable("Aucune note disponible");
        String result = Formatters.formatComputedResult(nc);
        assertEquals("Aucune note disponible", result);
    }

    @Test
    @DisplayName("Un résultat calculé provisoire comporte la mention provisoire")
    void testFormatProvisionalResult() {
        ComputedResult prov = ComputedResult.value(13.25, 20.0, ResultNature.PROVISIONAL, "prov");
        String result = Formatters.formatComputedResult(prov);
        assertTrue(result.contains("13,25 / 20 (prov.)"));
    }

    @Test
    @DisplayName("Formatage des statuts d'évaluation")
    void testFormatStatuses() {
        assertEquals("Planifiée", Formatters.formatEvaluationStatus(EvaluationStatus.PLANNED));
        assertEquals("Note en attente", Formatters.formatEvaluationStatus(EvaluationStatus.AWAITING_GRADE));
        assertEquals("Notée", Formatters.formatEvaluationStatus(EvaluationStatus.GRADED));
        assertEquals("Annulée", Formatters.formatEvaluationStatus(EvaluationStatus.CANCELLED));
    }

    @Test
    @DisplayName("Formatage des dates en français")
    void testFormatDate() {
        LocalDate date = LocalDate.of(2026, 11, 25);
        String formatted = Formatters.formatDate(Optional.of(date));
        assertTrue(formatted.contains("25") && formatted.contains("nov") && formatted.contains("2026"));
    }
}
