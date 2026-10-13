package application.engine;

import application.engine.config.*;
import application.engine.error.NotFoundException;
import application.engine.error.ValidationException;
import application.engine.model.*;
import application.services.GradeCalculationService;
import domain.entities.Annee;
import domain.entities.Note;
import domain.entities.Semestre;
import domain.entities.UE;
import infrastructure.persistence.DatabaseInitializer;
import infrastructure.persistence.JpaManager;
import infrastructure.repositories.AnneeRepository;
import infrastructure.repositories.NoteRepository;
import infrastructure.repositories.SemestreRepository;
import infrastructure.repositories.UeRepository;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Moteur de calcul réel et persistant MaNote.
 * Connecté à la base de données relationnelle locale via JPA et Hibernate.
 */
public class JpaGradeEngine implements GradeEngine {

    private final JpaManager jpaManager;
    private final AnneeRepository anneeRepository;
    private final SemestreRepository semestreRepository;
    private final UeRepository ueRepository;
    private final NoteRepository noteRepository;
    private final GradeCalculationService calculationService;

    public JpaGradeEngine() {
        this(JpaManager.getInstance());
    }

    public JpaGradeEngine(JpaManager jpaManager) {
        this.jpaManager = jpaManager;
        this.anneeRepository = new AnneeRepository(jpaManager);
        this.semestreRepository = new SemestreRepository(jpaManager);
        this.ueRepository = new UeRepository(jpaManager);
        this.noteRepository = new NoteRepository(jpaManager);
        this.calculationService = new GradeCalculationService();

        // Initialisation si base vierge
        DatabaseInitializer.initializeIfEmpty(jpaManager);
    }

    @Override
    public EngineInfo info() {
        return new EngineInfo(
                "MaNote JPA Engine (Base Réelle)",
                "2.0",
                "Moteur métier persistant connecté à la base relationnelle H2 (" + jpaManager.getDatabasePath() + ")",
                false
        );
    }

    @Override
    public EngineCapabilities capabilities() {
        return new EngineCapabilities(
                true, // structureEditing
                true, // evaluationWeights
                true, // contributions
                true, // averageTimeline
                true, // simulation
                Set.of(ConfigurationScope.Kind.GLOBAL, ConfigurationScope.Kind.SEMESTER)
        );
    }

    // ------------------------------------------------------------- Années & Semestres

    @Override
    public List<AcademicYearSummary> getAcademicYears() {
        List<Annee> annees = anneeRepository.findAll();
        List<AcademicYearSummary> result = new ArrayList<>();
        for (int i = 0; i < annees.size(); i++) {
            Annee a = annees.get(i);
            boolean isCurrent = (i == annees.size() - 1);

            double totalWeightedSem = 0.0;
            int computedSemCount = 0;
            int totalSemCount = a.getSemestres() != null ? a.getSemestres().size() : 0;

            if (a.getSemestres() != null) {
                for (Semestre s : a.getSemestres()) {
                    ComputedResult semRes = calculationService.computeSemesterAverage(s);
                    if (semRes instanceof ComputedResult.Value val) {
                        totalWeightedSem += val.value();
                        computedSemCount++;
                    }
                }
            }

            ComputedResult yearAvg;
            ValidationInfo yearVal;

            if (computedSemCount > 0) {
                double avgVal = totalWeightedSem / computedSemCount;
                ResultNature nat = (computedSemCount == totalSemCount) ? ResultNature.FINAL : ResultNature.PROVISIONAL;
                yearAvg = new ComputedResult.Value(avgVal, 20.0, nat,
                        String.format(Locale.FRENCH, "Moyenne annuelle calculée sur %d/%d semestre(s)", computedSemCount, totalSemCount));
                if (avgVal >= calculationService.getValidationThreshold()) {
                    yearVal = new ValidationInfo(ValidationState.VALIDATED, "Admis",
                            String.format(Locale.FRENCH, "Moyenne annuelle %.2f/20 >= %.1f", avgVal, calculationService.getValidationThreshold()));
                } else {
                    yearVal = new ValidationInfo(ValidationState.NOT_VALIDATED, "Ajourné",
                            String.format(Locale.FRENCH, "Moyenne annuelle %.2f/20 < %.1f", avgVal, calculationService.getValidationThreshold()));
                }
            } else {
                yearAvg = new ComputedResult.NotComputable("Moyenne annuelle en attente des résultats");
                yearVal = new ValidationInfo(ValidationState.IN_PROGRESS, "En cours", "Semestres en cours");
            }

            result.add(new AcademicYearSummary(
                    new AcademicYearId(a.getId()),
                    a.getNom(),
                    "Master 1 Informatique",
                    LocalDate.of(2025, 9, 1),
                    LocalDate.of(2026, 6, 30),
                    isCurrent,
                    yearAvg,
                    yearVal
            ));
        }
        return result;
    }

    @Override
    public List<SemesterSummary> getSemesters(AcademicYearId yearId) {
        List<Semestre> semestres = semestreRepository.findByAnneeId(yearId.value());
        return semestres.stream()
                .map(this::toSemesterSummary)
                .collect(Collectors.toList());
    }

    @Override
    public SemesterSummary getSemester(SemesterId semesterId) {
        Semestre s = semestreRepository.findById(semesterId.value())
                .orElseThrow(() -> new NotFoundException("Semestre", semesterId));
        return toSemesterSummary(s);
    }

    private SemesterSummary toSemesterSummary(Semestre s) {
        ComputedResult avg = calculationService.computeSemesterAverage(s);
        ValidationInfo val = calculationService.computeSemesterValidation(s, avg);
        int totalCredits = s.getUes().stream().mapToInt(UE::getCredit).sum();
        AcademicYearId yId = s.getAnnee() != null ? new AcademicYearId(s.getAnnee().getId()) : new AcademicYearId(1);

        List<Note> allNotes = s.getUes().stream()
                .flatMap(u -> u.getNotes().stream())
                .toList();
        EvaluationProgress progress = progressOf(allNotes);

        int semNum = Character.isDigit(s.getNum()) ? Character.getNumericValue(s.getNum()) : 1;
        LocalDate start = (semNum % 2 != 0) ? LocalDate.of(2025, 9, 1) : LocalDate.of(2026, 1, 15);
        LocalDate end = (semNum % 2 != 0) ? LocalDate.of(2026, 1, 10) : LocalDate.of(2026, 6, 15);

        return new SemesterSummary(
                new SemesterId(s.getId()),
                yId,
                semNum,
                s.getNom() != null ? s.getNom() : "Semestre " + s.getNum(),
                start,
                end,
                SemesterState.IN_PROGRESS,
                s.getUes().size(),
                totalCredits,
                avg,
                val,
                progress
        );
    }

    // ------------------------------------------------------------- UEs

    @Override
    public List<UeSummary> getUes(SemesterId semesterId) {
        List<UE> ues = ueRepository.findBySemestreId(semesterId.value());
        return ues.stream()
                .map(this::toUeSummary)
                .collect(Collectors.toList());
    }

    @Override
    public UeDetails getUe(UeId ueId) {
        UE ue = ueRepository.findById(ueId.value())
                .orElseThrow(() -> new NotFoundException("UE", ueId));

        UeSummary summary = toUeSummary(ue);
        List<EvaluationSummary> evalSummaries = ue.getNotes().stream()
                .map(n -> toEvaluationSummary(n, ue))
                .collect(Collectors.toList());

        List<EvaluationContribution> contributions = new ArrayList<>();
        double totalCoeff = ue.getNotes().stream().mapToDouble(Note::getCoefficient).sum();
        for (Note n : ue.getNotes()) {
            double coeff = n.getCoefficient() > 0 ? n.getCoefficient() : 1.0;
            OptionalDouble weightShare = totalCoeff > 0 ? OptionalDouble.of(coeff / totalCoeff) : OptionalDouble.empty();
            ComputedResult contr = calculationService.normalizeNoteOn20(n)
                    .map(v -> {
                        double shareVal = weightShare.isPresent() ? weightShare.getAsDouble() : 0.0;
                        return (ComputedResult) new ComputedResult.Value(v * shareVal, 20.0 * shareVal, ResultNature.FINAL, "Poids : " + Math.round(shareVal * 100) + "%");
                    })
                    .orElseGet(ComputedResult.NotProvided::new);

            contributions.add(new EvaluationContribution(
                    new EvaluationId(n.getId()),
                    n.getNom(),
                    weightShare,
                    contr
            ));
        }

        List<String> calcNotes = List.of(
                "Moyenne pondérée des notes enregistrées sur 20.",
                "Les coefficients sont pris en compte dans le calcul.",
                "Une note manquante n'est jamais assimilée à zéro."
        );

        return new UeDetails(
                summary,
                ue.getDescription() != null ? ue.getDescription() : "Aucune description renseignée.",
                evalSummaries,
                Optional.of(contributions),
                calcNotes
        );
    }

    private UeSummary toUeSummary(UE ue) {
        ComputedResult avg = calculationService.computeUeAverage(ue);
        ValidationInfo val = calculationService.computeUeValidation(avg, ue.getCredit());
        SemesterId semId = ue.getSemestre() != null ? new SemesterId(ue.getSemestre().getId()) : new SemesterId(1);

        EvaluationProgress progress = progressOf(ue.getNotes());

        Optional<String> attention = Optional.empty();
        if (avg instanceof ComputedResult.Value v && v.value() < 10.0) {
            attention = Optional.of("Moyenne actuelle inférieure à 10/20");
        } else if (progress.awaitingGrade() > 0) {
            attention = Optional.of(progress.awaitingGrade() + " note(s) en attente");
        }

        return new UeSummary(
                new UeId(ue.getId()),
                semId,
                ue.getCode(),
                ue.getNom(),
                ue.getCredit(),
                avg,
                val,
                progress,
                attention
        );
    }

    // ------------------------------------------------------------- Évaluations & Notes

    @Override
    public List<EvaluationType> getEvaluationTypes() {
        return List.of(
                new EvaluationType("DS", "Devoir Surveillé (DS)"),
                new EvaluationType("CC", "Contrôle Continu"),
                new EvaluationType("TP", "Travaux Pratiques"),
                new EvaluationType("PROJET", "Projet"),
                new EvaluationType("EXAMEN", "Examen Final"),
                new EvaluationType("PARTIEL", "Partiel"),
                new EvaluationType("QUIZ", "Quiz"),
                new EvaluationType("DEVOIR", "Devoir Maison")
        );
    }

    @Override
    public List<EvaluationSummary> getEvaluations(EvaluationFilter filter) {
        List<Note> allNotes = noteRepository.findAll();
        List<EvaluationSummary> list = new ArrayList<>();

        for (Note n : allNotes) {
            UE ue = n.getUe();
            if (ue == null) continue;
            EvaluationSummary summary = toEvaluationSummary(n, ue);
            if (filter.matches(summary)) {
                list.add(summary);
            }
        }
        return list;
    }

    @Override
    public EvaluationDetails getEvaluation(EvaluationId evaluationId) {
        Note n = noteRepository.findById(evaluationId.value())
                .orElseThrow(() -> new NotFoundException("Évaluation", evaluationId));
        UE ue = n.getUe();
        EvaluationSummary summary = toEvaluationSummary(n, ue != null ? ue : new UE("Inconnue", "INC", 0));

        ComputedResult contrib = calculationService.normalizeNoteOn20(n)
                .map(v -> (ComputedResult) new ComputedResult.Value(v, 20.0, ResultNature.PROVISIONAL, "Note ramenée sur 20"))
                .orElseGet(ComputedResult.NotProvided::new);

        LocalDate recordedDate = n.getDate() != null
                ? n.getDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
                : null;

        return new EvaluationDetails(summary, n.getAppreciation(), Optional.ofNullable(recordedDate), contrib);
    }

    private EvaluationSummary toEvaluationSummary(Note n, UE ue) {
        UeRef ueRef = new UeRef(new UeId(ue.getId()), ue.getCode(), ue.getNom());
        SemesterId semId = ue.getSemestre() != null ? new SemesterId(ue.getSemestre().getId()) : new SemesterId(1);

        EvaluationType type = new EvaluationType(n.getType() != null ? n.getType() : "CC", n.getType() != null ? n.getType() : "CC");

        EvaluationStatus status = statusOf(n);

        LocalDate date = null;
        if (n.getDate() != null) {
            date = n.getDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        }

        // La note n'est exposée que si le statut est GRADED (invariant du contrat)
        GradeEntry gradeEntry = null;
        if (status == EvaluationStatus.GRADED) {
            if (n.isAbsent()) {
                gradeEntry = new GradeEntry.Absence(n.isJustified());
            } else if (n.getValeur() != null) {
                gradeEntry = new GradeEntry.Score(n.getValeur(), n.getBareme() > 0 ? n.getBareme() : 20.0);
            }
        }

        ComputedResult normalized = status == EvaluationStatus.GRADED
                ? calculationService.normalizeNoteOn20(n)
                .map(v -> (ComputedResult) new ComputedResult.Value(v, 20.0, ResultNature.FINAL, "Normalisée /20"))
                .orElseGet(() -> new ComputedResult.NotComputable("Note non disponible"))
                : new ComputedResult.NotComputable("Note non disponible");

        return new EvaluationSummary(
                new EvaluationId(n.getId()),
                n.getNom(),
                type,
                ueRef,
                semId,
                Optional.ofNullable(date),
                status,
                n.getBareme() > 0 ? n.getBareme() : 20.0,
                OptionalDouble.of(n.getCoefficient() > 0 ? n.getCoefficient() : 1.0),
                Optional.ofNullable(gradeEntry),
                normalized
        );
    }

    @Override
    public EvaluationId createEvaluation(EvaluationDraft draft) {
        if (draft.title() == null || draft.title().isBlank()) {
            throw new ValidationException("title", "Le titre de l'évaluation ne peut pas être vide");
        }

        UE ue = ueRepository.findById(draft.ueId().value())
                .orElseThrow(() -> new NotFoundException("UE", draft.ueId()));

        Note n = new Note();
        n.setNom(draft.title().trim());
        n.setType(draft.typeCode() != null ? draft.typeCode() : "CC");
        n.setBareme(draft.maxPoints() > 0 ? draft.maxPoints() : 20.0);
        n.setCoefficient(draft.weight().isPresent() ? draft.weight().getAsDouble() : 1.0);
        n.setStatus(draft.status() != null ? draft.status().name() : "PLANNED");
        n.setAppreciation(draft.description());

        if (draft.date().isPresent()) {
            n.setDate(Date.from(draft.date().get().atStartOfDay(ZoneId.systemDefault()).toInstant()));
        } else {
            n.setDate(new Date());
        }

        ue.addNote(n);
        Note saved = noteRepository.save(n);
        return new EvaluationId(saved.getId());
    }

    @Override
    public void updateEvaluation(EvaluationId evaluationId, EvaluationDraft draft) {
        Note n = noteRepository.findById(evaluationId.value())
                .orElseThrow(() -> new NotFoundException("Évaluation", evaluationId));

        if (draft.title() != null && !draft.title().isBlank()) {
            n.setNom(draft.title().trim());
        }
        if (draft.typeCode() != null) {
            n.setType(draft.typeCode());
        }
        if (draft.maxPoints() > 0) {
            n.setBareme(draft.maxPoints());
        }
        if (draft.weight().isPresent()) {
            n.setCoefficient(draft.weight().getAsDouble());
        }
        if (draft.status() != null) {
            n.setStatus(draft.status().name());
        }
        if (draft.description() != null) {
            n.setAppreciation(draft.description());
        }
        if (draft.date().isPresent()) {
            n.setDate(Date.from(draft.date().get().atStartOfDay(ZoneId.systemDefault()).toInstant()));
        }

        noteRepository.save(n);
    }

    @Override
    public void deleteEvaluation(EvaluationId evaluationId) {
        noteRepository.delete(evaluationId.value());
    }

    @Override
    public void recordGrade(EvaluationId evaluationId, GradeEntry grade) {
        Note n = noteRepository.findById(evaluationId.value())
                .orElseThrow(() -> new NotFoundException("Évaluation", evaluationId));

        if (grade instanceof GradeEntry.Score s) {
            if (s.points() < 0 || s.points() > s.outOf()) {
                throw new ValidationException("score", "La note doit être entre 0 et " + s.outOf());
            }
            n.setValeur(s.points());
            n.setBareme(s.outOf());
            n.setAbsent(false);
            n.setJustified(false);
            n.setStatus("GRADED");
        } else if (grade instanceof GradeEntry.Absence a) {
            n.setValeur(null);
            n.setAbsent(true);
            n.setJustified(a.justified());
            n.setStatus("GRADED");
        }

        noteRepository.save(n);
    }

    @Override
    public void clearGrade(EvaluationId evaluationId) {
        Note n = noteRepository.findById(evaluationId.value())
                .orElseThrow(() -> new NotFoundException("Évaluation", evaluationId));

        n.setValeur(null);
        n.setAbsent(false);
        n.setJustified(false);
        n.setStatus("AWAITING_GRADE");

        noteRepository.save(n);
    }

    // ------------------------------------------------------------- Analyse, Simulation & Timeline

    @Override
    public List<TimelinePoint> getAverageTimeline(SemesterId semesterId) {
        Semestre s = semestreRepository.findById(semesterId.value())
                .orElseThrow(() -> new NotFoundException("Semestre", semesterId));
        return calculationService.computeAverageTimeline(s);
    }

    @Override
    public SimulationResult simulate(SimulationRequest request) {
        Semestre s = semestreRepository.findById(request.semesterId().value())
                .orElseThrow(() -> new NotFoundException("Semestre", request.semesterId()));
        return calculationService.simulate(s, request);
    }

    // ------------------------------------------------------------- Configuration Dynamique

    @Override
    public ConfigurationView getConfiguration(ConfigurationScope scope) {
        List<ConfigurationParameter> params = List.of(
                new ConfigurationParameter(
                        "validation.threshold",
                        "Seuil de validation",
                        "Note minimale sur 20 requise pour valider une UE ou le semestre.",
                        ConfigValue.number(calculationService.getValidationThreshold()),
                        ParameterConstraints.number(0.0, 20.0, 0.5, "/20"),
                        true,
                        Optional.empty()
                ),
                new ConfigurationParameter(
                        "absence.unjustified_as_zero",
                        "Assimiler ABI à zéro",
                        "Si activé, une absence injustifiée (ABI) équivaut à 0/20 dans le calcul.",
                        ConfigValue.bool(calculationService.isCountUnjustifiedAbsenceAsZero()),
                        ParameterConstraints.none(),
                        true,
                        Optional.empty()
                )
        );

        ConfigurationSection section = new ConfigurationSection(
                "regles_calcul",
                "Règles Académiques de Calcul",
                "Paramètres généraux du moteur",
                params
        );

        return new ConfigurationView(scope, "Configuration du Moteur", "Paramètres du moteur", List.of(section));
    }

    @Override
    public void updateConfiguration(ConfigurationScope scope, Map<String, ConfigValue> changes) {
        if (changes.containsKey("validation.threshold")) {
            ConfigValue v = changes.get("validation.threshold");
            if (v instanceof ConfigValue.NumberValue nv) {
                calculationService.setValidationThreshold(nv.value());
            }
        }
        if (changes.containsKey("absence.unjustified_as_zero")) {
            ConfigValue v = changes.get("absence.unjustified_as_zero");
            if (v instanceof ConfigValue.BooleanValue bv) {
                calculationService.setCountUnjustifiedAbsenceAsZero(bv.value());
            }
        }
    }

    @Override
    public void resetConfiguration(ConfigurationScope scope) {
        calculationService.setValidationThreshold(10.0);
        calculationService.setCountUnjustifiedAbsenceAsZero(true);
    }

    // ------------------------------------------------------------- Édition Structurelle

    @Override
    public AcademicYearId createAcademicYear(AcademicYearDraft draft) {
        Annee a = new Annee(draft.label());
        Annee saved = anneeRepository.save(a);
        return new AcademicYearId(saved.getId());
    }

    @Override
    public void updateAcademicYear(AcademicYearId yearId, AcademicYearDraft draft) {
        Annee a = anneeRepository.findById(yearId.value())
                .orElseThrow(() -> new NotFoundException("Année", yearId));
        a.setNom(draft.label());
        anneeRepository.save(a);
    }

    @Override
    public void deleteAcademicYear(AcademicYearId yearId) {
        anneeRepository.delete(yearId.value());
    }

    @Override
    public SemesterId createSemester(SemesterDraft draft) {
        Annee a = anneeRepository.findById(draft.yearId().value())
                .orElseThrow(() -> new NotFoundException("Année", draft.yearId()));
        char num = (char) ('0' + Math.min(9, Math.max(1, draft.number())));
        Semestre s = new Semestre(num, draft.label());
        a.addSemestre(s);
        Semestre saved = semestreRepository.save(s);
        return new SemesterId(saved.getId());
    }

    @Override
    public void updateSemester(SemesterId semesterId, SemesterDraft draft) {
        Semestre s = semestreRepository.findById(semesterId.value())
                .orElseThrow(() -> new NotFoundException("Semestre", semesterId));
        s.setNom(draft.label());
        semestreRepository.save(s);
    }

    @Override
    public void deleteSemester(SemesterId semesterId) {
        semestreRepository.delete(semesterId.value());
    }

    @Override
    public UeId createUe(UeDraft draft) {
        Semestre s = semestreRepository.findById(draft.semesterId().value())
                .orElseThrow(() -> new NotFoundException("Semestre", draft.semesterId()));
        UE ue = new UE(draft.name(), draft.code(), draft.credits());
        ue.setDescription(draft.description());
        s.addUe(ue);
        UE saved = ueRepository.save(ue);
        return new UeId(saved.getId());
    }

    @Override
    public void updateUe(UeId ueId, UeDraft draft) {
        UE ue = ueRepository.findById(ueId.value())
                .orElseThrow(() -> new NotFoundException("UE", ueId));
        ue.setNom(draft.name());
        ue.setCode(draft.code());
        ue.setCredit(draft.credits());
        ue.setDescription(draft.description());
        ueRepository.save(ue);
    }

    @Override
    public void deleteUe(UeId ueId) {
        ueRepository.delete(ueId.value());
    }


    // ------------------------------------------------------------- Helpers

    /**
     * Statut cohérent avec l'invariant du contrat : une note existe si et seulement si le statut est GRADED.
     */
    private static EvaluationStatus statusOf(Note n) {
        EvaluationStatus status;
        try {
            status = EvaluationStatus.valueOf(n.getStatus().toUpperCase());
        } catch (Exception e) {
            status = EvaluationStatus.PLANNED;
        }
        if (status == EvaluationStatus.CANCELLED) {
            return EvaluationStatus.CANCELLED;
        }
        boolean hasResult = n.isAbsent() || n.getValeur() != null;
        if (hasResult) {
            return EvaluationStatus.GRADED;
        }
        // Marquée "notée" mais sans résultat : on la considère en attente de note
        return status == EvaluationStatus.GRADED ? EvaluationStatus.AWAITING_GRADE : status;
    }

    /**
     * Décompte par statut. Ordre du constructeur : (planned, awaitingGrade, graded, cancelled).
     */
    private static EvaluationProgress progressOf(Collection<Note> notes) {
        int planned = 0, awaiting = 0, graded = 0, cancelled = 0;
        for (Note n : notes) {
            switch (statusOf(n)) {
                case PLANNED -> planned++;
                case AWAITING_GRADE -> awaiting++;
                case GRADED -> graded++;
                case CANCELLED -> cancelled++;
            }
        }
        return new EvaluationProgress(planned, awaiting, graded, cancelled);
    }
}
