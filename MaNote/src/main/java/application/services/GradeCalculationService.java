package application.services;

import application.engine.model.*;
import domain.entities.Note;
import domain.entities.Semestre;
import domain.entities.UE;

import java.time.ZoneId;
import java.util.*;

/**
 * Service métier de calcul des moyennes, de normalisation et de validation académique.
 * Règle d'or : une note non encore attribuée n'est JAMAIS assimilée à zéro.
 */
public class GradeCalculationService {

    private double validationThreshold = 10.0;
    private boolean countUnjustifiedAbsenceAsZero = true;

    public GradeCalculationService() {
    }

    public double getValidationThreshold() {
        return validationThreshold;
    }

    public void setValidationThreshold(double validationThreshold) {
        this.validationThreshold = validationThreshold;
    }

    public boolean isCountUnjustifiedAbsenceAsZero() {
        return countUnjustifiedAbsenceAsZero;
    }

    public void setCountUnjustifiedAbsenceAsZero(boolean countUnjustifiedAbsenceAsZero) {
        this.countUnjustifiedAbsenceAsZero = countUnjustifiedAbsenceAsZero;
    }

    /**
     * Normalise une note sur 20.
     */
    public Optional<Double> normalizeNoteOn20(Note note) {
        if (note == null) return Optional.empty();
        if (note.isAbsent()) {
            if (!note.isJustified() && countUnjustifiedAbsenceAsZero) {
                return Optional.of(0.0);
            }
            return Optional.empty();
        }
        if (note.getValeur() == null) {
            return Optional.empty();
        }
        double bareme = note.getBareme() > 0 ? note.getBareme() : 20.0;
        double norm = (note.getValeur() / bareme) * 20.0;
        return Optional.of(Math.max(0.0, Math.min(20.0, norm)));
    }

    /**
     * Calcule la moyenne d'une UE à partir de ses notes.
     */
    public ComputedResult computeUeAverage(UE ue) {
        List<Note> notes = ue.getNotes();
        if (notes == null || notes.isEmpty()) {
            return new ComputedResult.NotComputable("Aucune évaluation prévue pour cette UE");
        }

        double totalWeightedPoints = 0.0;
        double totalCoeff = 0.0;
        int gradedCount = 0;
        int totalCount = notes.size();

        for (Note note : notes) {
            Optional<Double> normOpt = normalizeNoteOn20(note);
            if (normOpt.isPresent()) {
                double coeff = note.getCoefficient() > 0 ? note.getCoefficient() : 1.0;
                totalWeightedPoints += normOpt.get() * coeff;
                totalCoeff += coeff;
                gradedCount++;
            }
        }

        if (totalCoeff <= 0.0 || gradedCount == 0) {
            return new ComputedResult.NotComputable("Aucune note enregistrée pour le moment");
        }

        double average = totalWeightedPoints / totalCoeff;
        ResultNature nature = (gradedCount == totalCount) ? ResultNature.FINAL : ResultNature.PROVISIONAL;
        String explanation = String.format(Locale.FRENCH,
                "Moyenne %s basée sur %d/%d note(s) (somme pondérée : %.2f / coeff : %.2f)",
                nature == ResultNature.FINAL ? "définitive" : "provisoire",
                gradedCount, totalCount, totalWeightedPoints, totalCoeff);

        return new ComputedResult.Value(average, 20.0, nature, explanation);
    }

    /**
     * Calcule la moyenne générale d'un semestre à partir de ses UE.
     */
    public ComputedResult computeSemesterAverage(Semestre semestre) {
        List<UE> ues = semestre.getUes();
        if (ues == null || ues.isEmpty()) {
            return new ComputedResult.NotComputable("Aucune UE dans ce semestre");
        }

        double totalWeightedUe = 0.0;
        double totalCredits = 0.0;
        boolean hasProvisional = false;
        int computedUeCount = 0;

        for (UE ue : ues) {
            ComputedResult res = computeUeAverage(ue);
            if (res instanceof ComputedResult.Value val) {
                int credits = ue.getCredit() > 0 ? ue.getCredit() : 1;
                totalWeightedUe += val.value() * credits;
                totalCredits += credits;
                computedUeCount++;
                if (val.nature() != ResultNature.FINAL) {
                    hasProvisional = true;
                }
            }
        }

        if (totalCredits <= 0.0 || computedUeCount == 0) {
            return new ComputedResult.NotComputable("Aucune note disponible dans ce semestre");
        }

        double semAverage = totalWeightedUe / totalCredits;
        ResultNature nature = (computedUeCount == ues.size() && !hasProvisional)
                ? ResultNature.FINAL
                : ResultNature.PROVISIONAL;
        String explanation = String.format(Locale.FRENCH,
                "Moyenne générale pondérée par les crédits ECTS (%d/%d UE calculées)",
                computedUeCount, ues.size());

        return new ComputedResult.Value(semAverage, 20.0, nature, explanation);
    }

    /**
     * Détermine les informations de validation d'une UE.
     */
    public ValidationInfo computeUeValidation(ComputedResult ueResult, int credits) {
        if (!(ueResult instanceof ComputedResult.Value val)) {
            return new ValidationInfo(ValidationState.IN_PROGRESS, "En cours", "Résultats en attente");
        }

        if (val.value() >= validationThreshold) {
            return new ValidationInfo(ValidationState.VALIDATED, "Validée",
                    String.format(Locale.FRENCH, "Moyenne %.2f/20 >= seuil %.1f (%d ECTS)", val.value(), validationThreshold, credits));
        } else {
            return new ValidationInfo(ValidationState.NOT_VALIDATED, "Ajournée",
                    String.format(Locale.FRENCH, "Moyenne %.2f/20 < seuil %.1f", val.value(), validationThreshold));
        }
    }

    /**
     * Détermine les informations de validation du semestre.
     */
    public ValidationInfo computeSemesterValidation(Semestre semestre, ComputedResult semResult) {
        List<UE> ues = semestre.getUes();

        if (!(semResult instanceof ComputedResult.Value val)) {
            return new ValidationInfo(ValidationState.IN_PROGRESS, "En cours", "Évaluations en cours");
        }

        boolean allUePassed = true;

        for (UE ue : ues) {
            ComputedResult ur = computeUeAverage(ue);
            if (!(ur instanceof ComputedResult.Value uv && uv.value() >= validationThreshold)) {
                allUePassed = false;
            }
        }

        if (val.value() >= validationThreshold) {
            if (allUePassed) {
                return new ValidationInfo(ValidationState.VALIDATED, "Validé", "Semestre validé avec succès");
            } else {
                return new ValidationInfo(ValidationState.COMPENSATED, "Compensé", "Validé par compensation semestrielle");
            }
        } else {
            return new ValidationInfo(ValidationState.NOT_VALIDATED, "Ajourné", "Semestre non validé à ce jour");
        }
    }

    /**
     * Calcule la chronologie de l'évolution de la moyenne au fil des notes obtenues.
     */
    public List<TimelinePoint> computeAverageTimeline(Semestre semestre) {
        List<Note> gradedNotes = new ArrayList<>();
        for (UE ue : semestre.getUes()) {
            for (Note n : ue.getNotes()) {
                if (n.getDate() != null && (n.getValeur() != null || n.isAbsent())) {
                    gradedNotes.add(n);
                }
            }
        }

        gradedNotes.sort(Comparator.comparing(Note::getDate));
        if (gradedNotes.isEmpty()) {
            return List.of();
        }

        List<TimelinePoint> points = new ArrayList<>();
        double runningPoints = 0.0;
        double runningCoeff = 0.0;

        for (Note n : gradedNotes) {
            Optional<Double> norm = normalizeNoteOn20(n);
            if (norm.isPresent()) {
                double coeff = n.getCoefficient() > 0 ? n.getCoefficient() : 1.0;
                runningPoints += norm.get() * coeff;
                runningCoeff += coeff;
                double currentAvg = runningPoints / runningCoeff;
                java.time.LocalDate localDate = n.getDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                points.add(new TimelinePoint(localDate, n.getNom(),
                        new ComputedResult.Value(currentAvg, 20.0, ResultNature.PROVISIONAL, "Moyenne : " + String.format(Locale.FRENCH, "%.2f", currentAvg))));
            }
        }

        return points;
    }

    /**
     * Simule les moyennes avec des hypothèses de notes.
     */
    public SimulationResult simulate(Semestre semestre, SimulationRequest request) {
        double currentAverage = 0.0;
        ComputedResult baseRes = computeSemesterAverage(semestre);
        if (baseRes instanceof ComputedResult.Value val) {
            currentAverage = val.value();
        }

        double totalWeightedUe = 0.0;
        double totalCredits = 0.0;
        Map<UeId, ComputedResult> simulatedUeAverages = new HashMap<>();
        Map<UeId, ValidationInfo> simulatedUeValidations = new HashMap<>();

        for (UE ue : semestre.getUes()) {
            double totalPoints = 0.0;
            double totalCoeff = 0.0;

            for (Note n : ue.getNotes()) {
                double coeff = n.getCoefficient() > 0 ? n.getCoefficient() : 1.0;
                EvaluationId evalId = new EvaluationId(n.getId());

                if (request.hypotheses().containsKey(evalId)) {
                    GradeEntry hyp = request.hypotheses().get(evalId);
                    if (hyp instanceof GradeEntry.Score s) {
                        double norm = (s.points() / (s.outOf() > 0 ? s.outOf() : 20.0)) * 20.0;
                        totalPoints += norm * coeff;
                        totalCoeff += coeff;
                    }
                } else {
                    Optional<Double> norm = normalizeNoteOn20(n);
                    if (norm.isPresent()) {
                        totalPoints += norm.get() * coeff;
                        totalCoeff += coeff;
                    }
                }
            }

            double ueSimAvg = totalCoeff > 0 ? (totalPoints / totalCoeff) : 10.0;
            ComputedResult ueRes = new ComputedResult.Value(ueSimAvg, 20.0, ResultNature.SIMULATED, "Moyenne simulée");
            UeId ueId = new UeId(ue.getId());
            simulatedUeAverages.put(ueId, ueRes);
            simulatedUeValidations.put(ueId, computeUeValidation(ueRes, ue.getCredit()));

            int credits = ue.getCredit() > 0 ? ue.getCredit() : 1;
            totalWeightedUe += ueSimAvg * credits;
            totalCredits += credits;
        }

        double simulatedGeneralAverage = totalCredits > 0 ? (totalWeightedUe / totalCredits) : currentAverage;
        ComputedResult semSimRes = new ComputedResult.Value(simulatedGeneralAverage, 20.0, ResultNature.SIMULATED, "Moyenne générale simulée");

        return new SimulationResult(
                semSimRes,
                simulatedUeAverages,
                simulatedUeValidations,
                List.of("Simulation basée sur " + request.hypotheses().size() + " note(s) hypothétique(s)")
        );
    }
}
