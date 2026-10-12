package infrastructure.demo;

import application.engine.EngineCapabilities;
import application.engine.EngineInfo;
import application.engine.GradeEngine;
import application.engine.config.*;
import application.engine.error.FieldError;
import application.engine.error.NotFoundException;
import application.engine.error.ValidationException;
import application.engine.model.*;

import java.time.LocalDate;
import java.util.*;

/**
 * Implémentation de démonstration du contrat {@link GradeEngine}.
 * <p>
 * <strong>IMPORTANT :</strong> Cette classe fournit un jeu de données fictif et des calculs
 * de démonstration pour permettre le développement et le test de l'interface graphique.
 * Elle est explicitement signalée comme moteur de démonstration ({@link EngineInfo#demonstration()} = true).
 * Elle implémente toutes les capacités (simulation, timeline, configuration, CRUD) pour valider l'UI.
 */
public class DemoGradeEngine implements GradeEngine {

    private final DemoData data;
    private final LocalDate today;

    // Configuration en mémoire
    private final Map<String, ConfigValue> globalConfig = new LinkedHashMap<>();
    private final Map<SemesterId, Map<String, ConfigValue>> semesterConfigs = new HashMap<>();
    private final Map<UeId, Map<String, ConfigValue>> ueConfigs = new HashMap<>();

    public DemoGradeEngine() {
        this(LocalDate.now());
    }

    public DemoGradeEngine(LocalDate today) {
        this.today = today;
        this.data = DemoData.generate(today);
        initDefaultConfig();
    }

    private void initDefaultConfig() {
        globalConfig.put("validation_threshold", ConfigValue.number(10.0));
        globalConfig.put("warning_threshold", ConfigValue.number(10.0));
        globalConfig.put("unexcused_absence_rule", ConfigValue.choice("ZERO"));
        globalConfig.put("compensation_enabled", ConfigValue.bool(true));
        globalConfig.put("display_scale", ConfigValue.number(20.0));
    }

    @Override
    public EngineInfo info() {
        return new EngineInfo(
                "Moteur de démonstration (MaNote Demo Engine)",
                "1.0.0-DEMO",
                "Fournit un jeu de données fictif complet pour la validation de l'interface utilisateur. " +
                        "Ce moteur sera remplacé par le moteur de calcul définitif de l'étudiant.",
                true
        );
    }

    @Override
    public EngineCapabilities capabilities() {
        return new EngineCapabilities(
                true,   // structureEditing
                true,   // evaluationWeights
                true,   // contributions
                true,   // averageTimeline
                true,   // simulation
                Set.of(ConfigurationScope.Kind.GLOBAL, ConfigurationScope.Kind.SEMESTER, ConfigurationScope.Kind.UE)
        );
    }

    // =====================================================================================
    // Structure académique
    // =====================================================================================

    @Override
    public synchronized List<AcademicYearSummary> getAcademicYears() {
        List<AcademicYearSummary> result = new ArrayList<>();
        // Plus récente à plus ancienne
        List<DemoData.Year> list = new ArrayList<>(data.years.values());
        list.sort((a, b) -> b.start.compareTo(a.start));

        boolean first = true;
        for (DemoData.Year y : list) {
            boolean current = first;
            first = false;
            AcademicYearId yId = new AcademicYearId(y.id);
            ComputedResult avg = calculateYearAverage(yId);
            ValidationInfo val = evaluateYearValidation(avg);
            result.add(new AcademicYearSummary(yId, y.label, y.programme, y.start, y.end, current, avg, val));
        }
        return result;
    }

    @Override
    public synchronized List<SemesterSummary> getSemesters(AcademicYearId yearId) {
        List<DemoData.Semester> sems = data.semestersOf(yearId.value());
        List<SemesterSummary> result = new ArrayList<>();
        for (DemoData.Semester s : sems) {
            result.add(buildSemesterSummary(s));
        }
        return result;
    }

    @Override
    public synchronized SemesterSummary getSemester(SemesterId semesterId) {
        DemoData.Semester s = data.semesters.get(semesterId.value());
        if (s == null) {
            throw new NotFoundException("Semestre", semesterId);
        }
        return buildSemesterSummary(s);
    }

    private SemesterSummary buildSemesterSummary(DemoData.Semester s) {
        SemesterId sId = new SemesterId(s.id);
        List<DemoData.Ue> ues = data.uesOf(s.id);
        int totalCredits = ues.stream().mapToInt(u -> u.credits).sum();

        List<EvaluationStatus> statuses = new ArrayList<>();
        for (DemoData.Ue u : ues) {
            for (DemoData.Evaluation e : data.evaluationsOf(u.id)) {
                statuses.add(e.status);
            }
        }
        EvaluationProgress progress = EvaluationProgress.of(statuses);

        SemesterState state;
        if (today.isBefore(s.start)) {
            state = SemesterState.UPCOMING;
        } else if (today.isAfter(s.end)) {
            state = SemesterState.COMPLETED;
        } else {
            state = SemesterState.IN_PROGRESS;
        }

        ComputedResult avg = calculateSemesterAverage(sId);
        ValidationInfo val = evaluateSemesterValidation(avg, state, progress);

        return new SemesterSummary(
                sId,
                new AcademicYearId(s.yearId),
                s.number,
                s.label,
                s.start,
                s.end,
                state,
                ues.size(),
                totalCredits,
                avg,
                val,
                progress
        );
    }

    @Override
    public synchronized List<UeSummary> getUes(SemesterId semesterId) {
        List<DemoData.Ue> ues = data.uesOf(semesterId.value());
        List<UeSummary> result = new ArrayList<>();
        for (DemoData.Ue u : ues) {
            result.add(buildUeSummary(u));
        }
        return result;
    }

    @Override
    public synchronized UeDetails getUe(UeId ueId) {
        DemoData.Ue u = data.ues.get(ueId.value());
        if (u == null) {
            throw new NotFoundException("UE", ueId);
        }
        UeSummary summary = buildUeSummary(u);
        List<DemoData.Evaluation> evals = data.evaluationsOf(u.id);
        evals.sort((a, b) -> {
            if (a.date == null && b.date == null) return 0;
            if (a.date == null) return 1;
            if (b.date == null) return -1;
            return a.date.compareTo(b.date);
        });

        List<EvaluationSummary> evalSummaries = new ArrayList<>();
        for (DemoData.Evaluation e : evals) {
            evalSummaries.add(buildEvaluationSummary(e, u));
        }

        List<EvaluationContribution> contributions = new ArrayList<>();
        double totalWeight = evals.stream()
                .filter(e -> e.status != EvaluationStatus.CANCELLED)
                .mapToDouble(e -> e.weight != null ? e.weight : 1.0)
                .sum();

        for (DemoData.Evaluation e : evals) {
            if (e.status == EvaluationStatus.CANCELLED) continue;
            double w = e.weight != null ? e.weight : 1.0;
            OptionalDouble weightShare = totalWeight > 0 ? OptionalDouble.of(w / totalWeight) : OptionalDouble.empty();

            ComputedResult contrib;
            if (e.status == EvaluationStatus.GRADED && e.grade instanceof GradeEntry.Score score) {
                double ptsOn20 = (score.points() / score.outOf()) * 20.0;
                double contribPts = totalWeight > 0 ? (ptsOn20 * w) / totalWeight : 0.0;
                contrib = ComputedResult.value(contribPts, 20.0, ResultNature.PROVISIONAL,
                        String.format("%.2f pts apportés sur 20", contribPts));
            } else if (e.status == EvaluationStatus.GRADED && e.grade instanceof GradeEntry.Absence ab) {
                contrib = ab.justified()
                        ? ComputedResult.notComputable("Absence justifiée (neutralisée)")
                        : ComputedResult.value(0.0, 20.0, ResultNature.PROVISIONAL, "Absence injustifiée comptée à 0");
            } else {
                contrib = ComputedResult.notComputable("Évaluation en attente de note");
            }

            contributions.add(new EvaluationContribution(new EvaluationId(e.id), e.title, weightShare, contrib));
        }

        List<String> calcNotes = List.of(
                "Moyenne calculée par moyenne pondérée des notes ramenées sur 20.",
                "Les quiz et TP contribuent selon leurs coefficients respectifs.",
                "Une absence justifiée (ABJ) est neutralisée dans le calcul de démonstration."
        );

        return new UeDetails(summary, u.description, evalSummaries, Optional.of(contributions), calcNotes);
    }

    private UeSummary buildUeSummary(DemoData.Ue u) {
        UeId ueId = new UeId(u.id);
        List<DemoData.Evaluation> evals = data.evaluationsOf(u.id);
        List<EvaluationStatus> statuses = evals.stream().map(e -> e.status).toList();
        EvaluationProgress progress = EvaluationProgress.of(statuses);

        ComputedResult avg = calculateUeAverage(u);
        ValidationInfo val = evaluateUeValidation(avg, progress);

        Optional<String> attention = Optional.empty();
        if (avg instanceof ComputedResult.Value v && v.value() < 10.0) {
            attention = Optional.of("Moyenne actuelle inférieure à 10/20");
        } else if (progress.awaitingGrade() > 0) {
            attention = Optional.of(progress.awaitingGrade() + " note(s) en attente");
        }

        return new UeSummary(
                ueId,
                new SemesterId(u.semesterId),
                u.code,
                u.name,
                u.credits,
                avg,
                val,
                progress,
                attention
        );
    }

    @Override
    public List<EvaluationType> getEvaluationTypes() {
        return data.types;
    }

    // =====================================================================================
    // Évaluations et notes
    // =====================================================================================

    @Override
    public synchronized List<EvaluationSummary> getEvaluations(EvaluationFilter filter) {
        List<EvaluationSummary> list = new ArrayList<>();
        for (DemoData.Evaluation e : data.evaluations.values()) {
            DemoData.Ue u = data.ues.get(e.ueId);
            if (u == null) continue;
            EvaluationSummary summary = buildEvaluationSummary(e, u);
            if (filter.matches(summary)) {
                list.add(summary);
            }
        }
        return list;
    }

    @Override
    public synchronized EvaluationDetails getEvaluation(EvaluationId evaluationId) {
        DemoData.Evaluation e = data.evaluations.get(evaluationId.value());
        if (e == null) {
            throw new NotFoundException("Évaluation", evaluationId);
        }
        DemoData.Ue u = data.ues.get(e.ueId);
        EvaluationSummary summary = buildEvaluationSummary(e, u);

        ComputedResult contrib = ComputedResult.notProvided();
        if (e.status == EvaluationStatus.GRADED && e.grade instanceof GradeEntry.Score score) {
            double ptsOn20 = (score.points() / score.outOf()) * 20.0;
            contrib = ComputedResult.value(ptsOn20, 20.0, ResultNature.PROVISIONAL, "Note sur 20");
        }

        return new EvaluationDetails(
                summary,
                e.description,
                Optional.ofNullable(e.gradeRecordedOn),
                contrib
        );
    }

    private EvaluationSummary buildEvaluationSummary(DemoData.Evaluation e, DemoData.Ue u) {
        EvaluationType type = data.types.stream()
                .filter(t -> t.code().equals(e.typeCode))
                .findFirst()
                .orElse(new EvaluationType(e.typeCode, e.typeCode));

        ComputedResult normalized;
        if (e.status == EvaluationStatus.GRADED && e.grade != null) {
            if (e.grade instanceof GradeEntry.Score score) {
                double valOn20 = (score.points() / score.outOf()) * 20.0;
                normalized = ComputedResult.value(valOn20, 20.0, ResultNature.PROVISIONAL,
                        String.format("%.2f / 20", valOn20));
            } else if (e.grade instanceof GradeEntry.Absence ab) {
                normalized = ab.justified()
                        ? ComputedResult.notComputable("Absence justifiée")
                        : ComputedResult.value(0.0, 20.0, ResultNature.PROVISIONAL, "0.00 / 20 (ABI)");
            } else {
                normalized = ComputedResult.notComputable("Non renseigné");
            }
        } else {
            normalized = ComputedResult.notComputable("Note non disponible");
        }

        return new EvaluationSummary(
                new EvaluationId(e.id),
                e.title,
                type,
                new UeRef(new UeId(u.id), u.code, u.name),
                new SemesterId(u.semesterId),
                Optional.ofNullable(e.date),
                e.status,
                e.maxPoints,
                e.weight != null ? OptionalDouble.of(e.weight) : OptionalDouble.empty(),
                Optional.ofNullable(e.grade),
                normalized
        );
    }

    @Override
    public synchronized EvaluationId createEvaluation(EvaluationDraft draft) {
        validateEvaluationDraft(draft);

        DemoData.Ue u = data.ues.get(draft.ueId().value());
        if (u == null) {
            throw new NotFoundException("UE", draft.ueId());
        }

        if (draft.status() == EvaluationStatus.GRADED) {
            throw new ValidationException("status",
                    "Une nouvelle évaluation ne peut pas être créée directement avec le statut GRADED. " +
                            "Créez-la en statut PLANNED ou AWAITING_GRADE puis enregistrez la note.");
        }

        DemoData.Evaluation e = new DemoData.Evaluation();
        e.id = data.nextId();
        e.ueId = u.id;
        e.title = draft.title();
        e.typeCode = draft.typeCode();
        e.date = draft.date().orElse(null);
        e.status = draft.status();
        e.maxPoints = draft.maxPoints();
        e.weight = draft.weight().isPresent() ? draft.weight().getAsDouble() : null;
        e.description = draft.description();
        e.grade = null;
        e.gradeRecordedOn = null;

        data.evaluations.put(e.id, e);
        return new EvaluationId(e.id);
    }

    @Override
    public synchronized void updateEvaluation(EvaluationId evaluationId, EvaluationDraft draft) {
        validateEvaluationDraft(draft);

        DemoData.Evaluation e = data.evaluations.get(evaluationId.value());
        if (e == null) {
            throw new NotFoundException("Évaluation", evaluationId);
        }

        DemoData.Ue u = data.ues.get(draft.ueId().value());
        if (u == null) {
            throw new NotFoundException("UE", draft.ueId());
        }

        if (draft.status() == EvaluationStatus.GRADED && e.grade == null) {
            throw new ValidationException("status",
                    "Impossible de passer en statut GRADED sans enregistrer de note au préalable.");
        }

        if (e.status == EvaluationStatus.GRADED && draft.status() != EvaluationStatus.GRADED) {
            throw new ValidationException("status",
                    "L'évaluation possède déjà une note. Retirez d'abord la note pour changer son statut.");
        }

        e.ueId = u.id;
        e.title = draft.title();
        e.typeCode = draft.typeCode();
        e.date = draft.date().orElse(null);
        e.status = draft.status();
        e.maxPoints = draft.maxPoints();
        e.weight = draft.weight().isPresent() ? draft.weight().getAsDouble() : null;
        e.description = draft.description();
    }

    private void validateEvaluationDraft(EvaluationDraft draft) {
        List<FieldError> errors = new ArrayList<>();
        if (draft.title() == null || draft.title().isBlank()) {
            errors.add(new FieldError("title", "Le titre de l'évaluation est obligatoire."));
        }
        if (draft.maxPoints() <= 0 || !Double.isFinite(draft.maxPoints())) {
            errors.add(new FieldError("maxPoints", "Le barème doit être un nombre strictement positif."));
        }
        if (draft.weight().isPresent()) {
            double w = draft.weight().getAsDouble();
            if (w < 0 || !Double.isFinite(w)) {
                errors.add(new FieldError("weight", "Le coefficient doit être positif ou nul."));
            }
        }
        if (draft.typeCode() == null || draft.typeCode().isBlank()) {
            errors.add(new FieldError("typeCode", "Le type d'évaluation doit être renseigné."));
        }
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
    }

    @Override
    public synchronized void deleteEvaluation(EvaluationId evaluationId) {
        if (data.evaluations.remove(evaluationId.value()) == null) {
            throw new NotFoundException("Évaluation", evaluationId);
        }
    }

    @Override
    public synchronized void recordGrade(EvaluationId evaluationId, GradeEntry grade) {
        DemoData.Evaluation e = data.evaluations.get(evaluationId.value());
        if (e == null) {
            throw new NotFoundException("Évaluation", evaluationId);
        }
        if (e.status == EvaluationStatus.CANCELLED) {
            throw new ValidationException("grade", "Impossible d'attribuer une note à une évaluation annulée.");
        }
        if (grade == null) {
            throw new ValidationException("grade", "La note ne peut pas être nulle.");
        }

        if (grade instanceof GradeEntry.Score score) {
            if (score.points() > e.maxPoints) {
                throw new ValidationException("points",
                        String.format("La note (%.2f) ne peut pas dépasser le barème de l'évaluation (%.2f).",
                                score.points(), e.maxPoints));
            }
        }

        e.grade = grade;
        e.status = EvaluationStatus.GRADED;
        e.gradeRecordedOn = today;
    }

    @Override
    public synchronized void clearGrade(EvaluationId evaluationId) {
        DemoData.Evaluation e = data.evaluations.get(evaluationId.value());
        if (e == null) {
            throw new NotFoundException("Évaluation", evaluationId);
        }
        e.grade = null;
        e.status = EvaluationStatus.AWAITING_GRADE;
        e.gradeRecordedOn = null;
    }

    // =====================================================================================
    // Structure académique — modification
    // =====================================================================================

    @Override
    public synchronized AcademicYearId createAcademicYear(AcademicYearDraft draft) {
        if (draft.label().isBlank()) {
            throw new ValidationException("label", "Le libellé de l'année est obligatoire.");
        }
        DemoData.Year y = data.year(draft.label(), draft.programme(), draft.startDate(), draft.endDate());
        return new AcademicYearId(y.id);
    }

    @Override
    public synchronized void updateAcademicYear(AcademicYearId yearId, AcademicYearDraft draft) {
        DemoData.Year y = data.years.get(yearId.value());
        if (y == null) throw new NotFoundException("Année", yearId);
        y.label = draft.label();
        y.programme = draft.programme();
        y.start = draft.startDate();
        y.end = draft.endDate();
    }

    @Override
    public synchronized void deleteAcademicYear(AcademicYearId yearId) {
        data.years.remove(yearId.value());
    }

    @Override
    public synchronized SemesterId createSemester(SemesterDraft draft) {
        DemoData.Year y = data.years.get(draft.yearId().value());
        if (y == null) throw new NotFoundException("Année", draft.yearId());
        DemoData.Semester s = data.semester(y, draft.number(), draft.label(), draft.startDate(), draft.endDate());
        return new SemesterId(s.id);
    }

    @Override
    public synchronized void updateSemester(SemesterId semesterId, SemesterDraft draft) {
        DemoData.Semester s = data.semesters.get(semesterId.value());
        if (s == null) throw new NotFoundException("Semestre", semesterId);
        s.number = draft.number();
        s.label = draft.label();
        s.start = draft.startDate();
        s.end = draft.endDate();
    }

    @Override
    public synchronized void deleteSemester(SemesterId semesterId) {
        data.semesters.remove(semesterId.value());
    }

    @Override
    public synchronized UeId createUe(UeDraft draft) {
        if (draft.code().isBlank()) throw new ValidationException("code", "Le code de l'UE est obligatoire.");
        if (draft.name().isBlank()) throw new ValidationException("name", "L'intitulé de l'UE est obligatoire.");
        DemoData.Semester s = data.semesters.get(draft.semesterId().value());
        if (s == null) throw new NotFoundException("Semestre", draft.semesterId());

        DemoData.Ue u = new DemoData.Ue();
        u.id = data.nextId();
        u.semesterId = s.id;
        u.code = draft.code();
        u.name = draft.name();
        u.credits = draft.credits();
        u.description = draft.description();
        data.ues.put(u.id, u);
        return new UeId(u.id);
    }

    @Override
    public synchronized void updateUe(UeId ueId, UeDraft draft) {
        DemoData.Ue u = data.ues.get(ueId.value());
        if (u == null) throw new NotFoundException("UE", ueId);
        u.code = draft.code();
        u.name = draft.name();
        u.credits = draft.credits();
        u.description = draft.description();
    }

    @Override
    public synchronized void deleteUe(UeId ueId) {
        DemoData.Ue u = data.ues.remove(ueId.value());
        if (u == null) throw new NotFoundException("UE", ueId);
        data.evaluations.values().removeIf(e -> e.ueId == ueId.value());
    }

    // =====================================================================================
    // Analyse et simulation
    // =====================================================================================

    @Override
    public synchronized List<TimelinePoint> getAverageTimeline(SemesterId semesterId) {
        List<DemoData.Ue> ues = data.uesOf(semesterId.value());
        List<DemoData.Evaluation> gradedEvals = new ArrayList<>();
        for (DemoData.Ue u : ues) {
            for (DemoData.Evaluation e : data.evaluationsOf(u.id)) {
                if (e.status == EvaluationStatus.GRADED && e.gradeRecordedOn != null && e.grade instanceof GradeEntry.Score) {
                    gradedEvals.add(e);
                }
            }
        }
        gradedEvals.sort(Comparator.comparing(e -> e.gradeRecordedOn));

        List<TimelinePoint> points = new ArrayList<>();
        double runningSum = 0;
        double runningCount = 0;

        for (DemoData.Evaluation e : gradedEvals) {
            GradeEntry.Score sc = (GradeEntry.Score) e.grade;
            double scoreOn20 = (sc.points() / sc.outOf()) * 20.0;
            runningSum += scoreOn20;
            runningCount += 1.0;
            double runningAvg = runningSum / runningCount;

            points.add(new TimelinePoint(
                    e.gradeRecordedOn,
                    e.title,
                    ComputedResult.value(runningAvg, 20.0, ResultNature.PROVISIONAL,
                            String.format("Moyenne cumulative : %.2f/20", runningAvg))
            ));
        }

        return points;
    }

    @Override
    public synchronized SimulationResult simulate(SimulationRequest request) {
        DemoData.Semester s = data.semesters.get(request.semesterId().value());
        if (s == null) {
            throw new NotFoundException("Semestre", request.semesterId());
        }

        List<DemoData.Ue> ues = data.uesOf(s.id);
        Map<UeId, ComputedResult> ueAverages = new LinkedHashMap<>();
        Map<UeId, ValidationInfo> ueValidations = new LinkedHashMap<>();

        double semSum = 0;
        int semCredits = 0;

        for (DemoData.Ue u : ues) {
            UeId uId = new UeId(u.id);
            List<DemoData.Evaluation> evals = data.evaluationsOf(u.id);

            double sum = 0;
            double weights = 0;

            for (DemoData.Evaluation e : evals) {
                if (e.status == EvaluationStatus.CANCELLED) continue;
                double w = e.weight != null ? e.weight : 1.0;

                GradeEntry grade = e.grade;
                if (request.hypotheses().containsKey(new EvaluationId(e.id))) {
                    grade = request.hypotheses().get(new EvaluationId(e.id));
                }

                if (grade instanceof GradeEntry.Score sc) {
                    double on20 = (sc.points() / sc.outOf()) * 20.0;
                    sum += on20 * w;
                    weights += w;
                }
            }

            if (weights > 0) {
                double avg = sum / weights;
                ComputedResult res = ComputedResult.value(avg, 20.0, ResultNature.SIMULATED,
                        String.format("Moyenne simulée : %.2f/20", avg));
                ueAverages.put(uId, res);
                ueValidations.put(uId, avg >= 10.0
                        ? ValidationInfo.of(ValidationState.VALIDATED, "Simulé validé", "≥ 10/20")
                        : ValidationInfo.of(ValidationState.AT_RISK, "Simulé non validé", "< 10/20"));
                semSum += avg * u.credits;
                semCredits += u.credits;
            } else {
                ueAverages.put(uId, ComputedResult.notComputable("Aucune note ni hypothèse"));
                ueValidations.put(uId, ValidationInfo.of(ValidationState.IN_PROGRESS, "En attente", ""));
            }
        }

        ComputedResult semAverage;
        if (semCredits > 0) {
            double semAvg = semSum / semCredits;
            semAverage = ComputedResult.value(semAvg, 20.0, ResultNature.SIMULATED,
                    String.format("Moyenne générale simulée : %.2f/20", semAvg));
        } else {
            semAverage = ComputedResult.notComputable("Simulation impossible sans notes");
        }

        List<String> assumptions = List.of(
                "Toutes les hypothèses saisies sont prises en compte comme des scores réels sur 20.",
                "Pondération identique à la configuration standard du semestre.",
                "Ces résultats sont des estimations indicatives et n'ont aucune valeur officielle."
        );

        return new SimulationResult(semAverage, ueAverages, ueValidations, assumptions);
    }

    // =====================================================================================
    // Configuration
    // =====================================================================================

    @Override
    public synchronized ConfigurationView getConfiguration(ConfigurationScope scope) {
        String title;
        String desc;
        Map<String, ConfigValue> effective = new LinkedHashMap<>(globalConfig);

        switch (scope) {
            case ConfigurationScope.Global g -> {
                title = "Configuration générale du moteur de calcul";
                desc = "Paramètres de calcul globaux appliqués par défaut à l'ensemble des semestres et UE.";
            }
            case ConfigurationScope.Semester sem -> {
                DemoData.Semester s = data.semesters.get(sem.id().value());
                title = "Configuration — " + (s != null ? s.label : sem.id().toString());
                desc = "Surcharge des paramètres pour ce semestre spécifique.";
                if (semesterConfigs.containsKey(sem.id())) {
                    effective.putAll(semesterConfigs.get(sem.id()));
                }
            }
            case ConfigurationScope.Ue ueScope -> {
                DemoData.Ue u = data.ues.get(ueScope.id().value());
                title = "Configuration — " + (u != null ? u.code + " (" + u.name + ")" : ueScope.id().toString());
                desc = "Surcharge des paramètres pour cette UE spécifique.";
                if (ueConfigs.containsKey(ueScope.id())) {
                    effective.putAll(ueConfigs.get(ueScope.id()));
                }
            }
            default -> {
                title = "Configuration du calcul";
                desc = "";
            }
        }

        List<ConfigurationParameter> pGeneral = List.of(
                new ConfigurationParameter("validation_threshold", "Seuil de validation",
                        "Note minimale requise pour valider une UE ou un semestre.",
                        effective.getOrDefault("validation_threshold", ConfigValue.number(10.0)),
                        ParameterConstraints.number(0, 20, 0.5, "/20"), true, Optional.empty()),
                new ConfigurationParameter("warning_threshold", "Seuil d'alerte",
                        "En dessous de ce seuil, une UE est signalée comme nécessitant une attention particulière.",
                        effective.getOrDefault("warning_threshold", ConfigValue.number(10.0)),
                        ParameterConstraints.number(0, 20, 0.5, "/20"), true, Optional.empty())
        );

        List<ConfigurationParameter> pAbsences = List.of(
                new ConfigurationParameter("unexcused_absence_rule", "Absence injustifiée (ABI)",
                        "Traitement réservé à une absence non justifiée lors d'une évaluation.",
                        effective.getOrDefault("unexcused_absence_rule", ConfigValue.choice("ZERO")),
                        ParameterConstraints.choices(List.of(
                                new Choice("ZERO", "Assimiler à 0/20", "Attribue la note 0 dans le barème."),
                                new Choice("BLOCKING", "Bloquante", "Empêche la validation automatique de l'UE."),
                                new Choice("NEUTRALIZE", "Neutraliser", "Exclut l'évaluation du calcul."))
                        ), true, Optional.empty()),
                new ConfigurationParameter("compensation_enabled", "Compensation inter-UE",
                        "Autorise la compensation semestrielle des notes entre les différentes UE.",
                        effective.getOrDefault("compensation_enabled", ConfigValue.bool(true)),
                        ParameterConstraints.none(), true, Optional.empty())
        );

        List<ConfigurationSection> sections = List.of(
                new ConfigurationSection("thresholds", "Seuils et validation",
                        "Règles d'attribution des statuts de validation.", pGeneral),
                new ConfigurationSection("rules", "Gestion des absences et compensation",
                        "Comportement appliqué aux cas particuliers.", pAbsences)
        );

        return new ConfigurationView(scope, title, desc, sections);
    }

    @Override
    public synchronized void updateConfiguration(ConfigurationScope scope, Map<String, ConfigValue> changes) {
        switch (scope) {
            case ConfigurationScope.Global g -> globalConfig.putAll(changes);
            case ConfigurationScope.Semester sem ->
                    semesterConfigs.computeIfAbsent(sem.id(), k -> new LinkedHashMap<>()).putAll(changes);
            case ConfigurationScope.Ue ueScope ->
                    ueConfigs.computeIfAbsent(ueScope.id(), k -> new LinkedHashMap<>()).putAll(changes);
            default -> throw new UnsupportedOperationException("Périmètre non supporté : " + scope);
        }
    }

    @Override
    public synchronized void resetConfiguration(ConfigurationScope scope) {
        switch (scope) {
            case ConfigurationScope.Global g -> initDefaultConfig();
            case ConfigurationScope.Semester sem -> semesterConfigs.remove(sem.id());
            case ConfigurationScope.Ue ueScope -> ueConfigs.remove(ueScope.id());
            default -> {
            }
        }
    }

    // =====================================================================================
    // Utilitaires de calcul internes
    // =====================================================================================

    private ComputedResult calculateUeAverage(DemoData.Ue u) {
        List<DemoData.Evaluation> evals = data.evaluationsOf(u.id);
        double sum = 0;
        double weights = 0;
        boolean hasGrades = false;
        boolean hasPending = false;

        for (DemoData.Evaluation e : evals) {
            if (e.status == EvaluationStatus.CANCELLED) continue;
            double w = e.weight != null ? e.weight : 1.0;

            if (e.status == EvaluationStatus.GRADED && e.grade != null) {
                if (e.grade instanceof GradeEntry.Score sc) {
                    double on20 = (sc.points() / sc.outOf()) * 20.0;
                    sum += on20 * w;
                    weights += w;
                    hasGrades = true;
                } else if (e.grade instanceof GradeEntry.Absence ab) {
                    if (!ab.justified()) {
                        // ABI = 0
                        weights += w;
                        hasGrades = true;
                    }
                    // ABJ est neutralisée
                }
            } else if (e.status == EvaluationStatus.AWAITING_GRADE || e.status == EvaluationStatus.PLANNED) {
                hasPending = true;
            }
        }

        if (!hasGrades) {
            return ComputedResult.notComputable("Aucune note disponible");
        }

        double avg = sum / weights;
        ResultNature nature = hasPending ? ResultNature.PROVISIONAL : ResultNature.FINAL;
        String note = nature == ResultNature.PROVISIONAL ? "Moyenne provisoire sur les notes obtenues" : "Moyenne définitive de l'UE";
        return ComputedResult.value(avg, 20.0, nature, note);
    }

    private ComputedResult calculateSemesterAverage(SemesterId sId) {
        List<DemoData.Ue> ues = data.uesOf(sId.value());
        double sum = 0;
        int credits = 0;
        boolean hasProvisional = false;
        boolean hasAny = false;

        for (DemoData.Ue u : ues) {
            ComputedResult r = calculateUeAverage(u);
            if (r instanceof ComputedResult.Value v) {
                sum += v.value() * u.credits;
                credits += u.credits;
                hasAny = true;
                if (v.nature() == ResultNature.PROVISIONAL) hasProvisional = true;
            }
        }

        if (!hasAny || credits == 0) {
            return ComputedResult.notComputable("Notes insuffisantes pour calculer la moyenne");
        }

        double avg = sum / credits;
        ResultNature nature = hasProvisional ? ResultNature.PROVISIONAL : ResultNature.FINAL;
        return ComputedResult.value(avg, 20.0, nature,
                nature == ResultNature.PROVISIONAL ? "Moyenne générale semestrielle provisoire" : "Moyenne générale définitive");
    }

    private ComputedResult calculateYearAverage(AcademicYearId yId) {
        List<DemoData.Semester> sems = data.semestersOf(yId.value());
        double sum = 0;
        int count = 0;
        boolean hasProvisional = false;

        for (DemoData.Semester s : sems) {
            ComputedResult r = calculateSemesterAverage(new SemesterId(s.id));
            if (r instanceof ComputedResult.Value v) {
                sum += v.value();
                count++;
                if (v.nature() == ResultNature.PROVISIONAL) hasProvisional = true;
            }
        }

        if (count == 0) {
            return ComputedResult.notComputable("Moyenne annuelle non calculable");
        }

        double avg = sum / count;
        ResultNature nature = hasProvisional ? ResultNature.PROVISIONAL : ResultNature.FINAL;
        return ComputedResult.value(avg, 20.0, nature, "Moyenne annuelle");
    }

    private ValidationInfo evaluateUeValidation(ComputedResult avg, EvaluationProgress progress) {
        if (avg instanceof ComputedResult.Value v) {
            if (v.nature() == ResultNature.FINAL) {
                return v.value() >= 10.0
                        ? ValidationInfo.of(ValidationState.VALIDATED, "Validée", "Moyenne définitive ≥ 10/20")
                        : ValidationInfo.of(ValidationState.NOT_VALIDATED, "Non validée", "Moyenne définitive < 10/20");
            } else {
                return v.value() >= 10.0
                        ? ValidationInfo.of(ValidationState.IN_PROGRESS, "En cours (favorable)", "Moyenne actuelle ≥ 10/20")
                        : ValidationInfo.of(ValidationState.AT_RISK, "À surveiller", "Moyenne actuelle < 10/20");
            }
        }
        return ValidationInfo.of(ValidationState.IN_PROGRESS, "En cours", "En attente des premières évaluations");
    }

    private ValidationInfo evaluateSemesterValidation(ComputedResult avg, SemesterState state, EvaluationProgress progress) {
        if (state == SemesterState.UPCOMING) {
            return ValidationInfo.of(ValidationState.IN_PROGRESS, "À venir", "Semestre non débuté");
        }
        if (avg instanceof ComputedResult.Value v) {
            if (state == SemesterState.COMPLETED && v.nature() == ResultNature.FINAL) {
                return v.value() >= 10.0
                        ? ValidationInfo.of(ValidationState.VALIDATED, "Semestre validé", "Moyenne ≥ 10/20")
                        : ValidationInfo.of(ValidationState.NOT_VALIDATED, "Non validé", "Moyenne < 10/20");
            } else {
                return v.value() >= 10.0
                        ? ValidationInfo.of(ValidationState.IN_PROGRESS, "En bonne voie", "Moyenne provisoire ≥ 10/20")
                        : ValidationInfo.of(ValidationState.AT_RISK, "Attention requise", "Moyenne provisoire < 10/20");
            }
        }
        return ValidationInfo.of(ValidationState.IN_PROGRESS, "En cours", "En attente d'évaluations");
    }

    private ValidationInfo evaluateYearValidation(ComputedResult avg) {
        if (avg instanceof ComputedResult.Value v) {
            if (v.nature() == ResultNature.FINAL) {
                return v.value() >= 10.0
                        ? ValidationInfo.of(ValidationState.VALIDATED, "Année validée", "Admis")
                        : ValidationInfo.of(ValidationState.NOT_VALIDATED, "Ajourné", "Moyenne < 10/20");
            } else {
                return v.value() >= 10.0
                        ? ValidationInfo.of(ValidationState.IN_PROGRESS, "En cours", "Moyenne provisoire ≥ 10/20")
                        : ValidationInfo.of(ValidationState.AT_RISK, "À surveiller", "Moyenne provisoire < 10/20");
            }
        }
        return ValidationInfo.of(ValidationState.IN_PROGRESS, "En cours", "");
    }
}
