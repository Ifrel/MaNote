package api.controllers;

import api.model.AppContext;
import api.model.NavigationDestination;
import application.engine.GradeEngine;
import application.engine.config.ConfigValue;
import application.engine.config.ConfigurationScope;
import application.engine.error.EngineException;
import application.engine.model.*;
import infrastructure.engine.EngineLoader;
import infrastructure.preferences.UserPreferences;
import javafx.application.Platform;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * Contrôleur principal de l'application (Architecture MVC).
 * <p>
 * Coordonne les flux entre les vues JavaFX et l'interface {@link GradeEngine}.
 * Tous les appels au moteur sont exécutés sur un thread de travail dédié afin de ne
 * jamais figer le JavaFX Application Thread, et les retours sont restitués via {@link Platform#runLater}.
 */
public class AppController {

    private final GradeEngine engine;
    private final AppContext context;
    private final ExecutorService worker = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "MaNote-Engine-Worker");
        t.setDaemon(true);
        return t;
    });

    public AppController() {
        this(EngineLoader.getEngine(), new AppContext());
    }

    public AppController(GradeEngine engine, AppContext context) {
        this.engine = engine;
        this.context = context;
    }

    public GradeEngine getEngine() {
        return engine;
    }

    public AppContext getContext() {
        return context;
    }

    public void loadInitialDataSynchronously() {
        try {
            List<AcademicYearSummary> years = engine.getAcademicYears();
            if (years.isEmpty()) {
                context.setActiveYear(null);
                context.setActiveSemester(null);
                return;
            }

            int savedYearId = UserPreferences.getActiveYearId(years.get(0).id().value());
            AcademicYearSummary activeY = years.stream()
                    .filter(y -> y.id().value() == savedYearId)
                    .findFirst()
                    .orElse(years.get(0));

            List<SemesterSummary> sems = engine.getSemesters(activeY.id());
            int savedSemId = UserPreferences.getActiveSemesterId(sems.isEmpty() ? -1 : sems.get(0).id().value());
            SemesterSummary activeS = sems.stream()
                    .filter(s -> s.id().value() == savedSemId)
                    .findFirst()
                    .orElse(sems.isEmpty() ? null : sems.get(0));

            context.setActiveYear(activeY);
            context.setActiveSemester(activeS);
        } catch (Exception e) {
            System.err.println("[AppController] Erreur de chargement initial : " + e.getMessage());
        }
    }

    /**
     * Initialise l'application de façon asynchrone si nécessaire.
     */
    public void loadInitialData(Runnable onComplete) {
        worker.submit(() -> {
            loadInitialDataSynchronously();
            if (onComplete != null) {
                Platform.runLater(onComplete);
            }
        });
    }

    public void refreshActiveSemester(Runnable onComplete) {
        SemesterSummary current = context.getActiveSemester();
        if (current == null) {
            if (onComplete != null) onComplete.run();
            return;
        }
        worker.submit(() -> {
            try {
                SemesterSummary updated = engine.getSemester(current.id());
                Platform.runLater(() -> {
                    context.setActiveSemester(updated);
                    if (onComplete != null) onComplete.run();
                });
            } catch (Exception e) {
                if (onComplete != null) Platform.runLater(onComplete);
            }
        });
    }

    public void selectYear(AcademicYearSummary year, Runnable onComplete) {
        context.setActiveYear(year);
        worker.submit(() -> {
            try {
                List<SemesterSummary> sems = engine.getSemesters(year.id());
                SemesterSummary first = sems.isEmpty() ? null : sems.get(0);
                Platform.runLater(() -> {
                    context.setActiveSemester(first);
                    if (onComplete != null) onComplete.run();
                });
            } catch (Exception e) {
                Platform.runLater(() -> context.showToast("Erreur de sélection de l'année : " + e.getMessage(), AppContext.ToastType.ERROR));
            }
        });
    }

    public void selectSemester(SemesterSummary semester, Runnable onComplete) {
        context.setActiveSemester(semester);
        if (onComplete != null) onComplete.run();
    }

    public void navigateTo(NavigationDestination dest) {
        context.setCurrentDestination(dest);
    }

    public void navigateToUe(UeId ueId) {
        context.setSelectedUeId(ueId);
        context.setCurrentDestination(NavigationDestination.UE_DETAILS);
    }

    public void toggleTheme() {
        UserPreferences.ThemeMode current = context.getThemeMode();
        UserPreferences.ThemeMode next = (current == UserPreferences.ThemeMode.DARK)
                ? UserPreferences.ThemeMode.LIGHT
                : UserPreferences.ThemeMode.DARK;
        context.setThemeMode(next);
    }

    // ------------------------------------------------------------------ Opérations asynchrones métier

    public void executeAsync(EngineTask task, Consumer<String> onSuccess, Consumer<String> onError) {
        worker.submit(() -> {
            try {
                task.run(engine);
                Platform.runLater(() -> {
                    if (onSuccess != null) onSuccess.accept("Opération effectuée avec succès.");
                });
            } catch (EngineException e) {
                Platform.runLater(() -> {
                    if (onError != null) onError.accept(e.getMessage());
                    else context.showToast(e.getMessage(), AppContext.ToastType.ERROR);
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    String msg = "Erreur inattendue : " + e.getMessage();
                    if (onError != null) onError.accept(msg);
                    else context.showToast(msg, AppContext.ToastType.ERROR);
                });
            }
        });
    }

    public void recordGrade(EvaluationId evalId, GradeEntry grade, Runnable onSuccess) {
        executeAsync(
                eng -> eng.recordGrade(evalId, grade),
                msg -> {
                    context.showToast("Note enregistrée avec succès !", AppContext.ToastType.SUCCESS);
                    refreshActiveSemester(() -> {
                        if (onSuccess != null) onSuccess.run();
                    });
                },
                null
        );
    }

    public void clearGrade(EvaluationId evalId, Runnable onSuccess) {
        executeAsync(
                eng -> eng.clearGrade(evalId),
                msg -> {
                    context.showToast("Note retirée.", AppContext.ToastType.INFO);
                    refreshActiveSemester(() -> {
                        if (onSuccess != null) onSuccess.run();
                    });
                },
                null
        );
    }

    public void deleteEvaluation(EvaluationId evalId, Runnable onSuccess) {
        executeAsync(
                eng -> eng.deleteEvaluation(evalId),
                msg -> {
                    context.showToast("Évaluation supprimée.", AppContext.ToastType.INFO);
                    refreshActiveSemester(() -> {
                        if (onSuccess != null) onSuccess.run();
                    });
                },
                null
        );
    }

    public void createEvaluation(EvaluationDraft draft, Consumer<EvaluationId> onSuccess) {
        worker.submit(() -> {
            try {
                EvaluationId id = engine.createEvaluation(draft);
                Platform.runLater(() -> {
                    context.showToast("Évaluation créée avec succès !", AppContext.ToastType.SUCCESS);
                    refreshActiveSemester(() -> {
                        if (onSuccess != null) onSuccess.accept(id);
                    });
                });
            } catch (EngineException e) {
                Platform.runLater(() -> context.showToast(e.getMessage(), AppContext.ToastType.ERROR));
            }
        });
    }

    public void updateEvaluation(EvaluationId id, EvaluationDraft draft, Runnable onSuccess) {
        executeAsync(
                eng -> eng.updateEvaluation(id, draft),
                msg -> {
                    context.showToast("Évaluation mise à jour.", AppContext.ToastType.SUCCESS);
                    refreshActiveSemester(() -> {
                        if (onSuccess != null) onSuccess.run();
                    });
                },
                null
        );
    }

    public void updateConfiguration(ConfigurationScope scope, Map<String, ConfigValue> changes, Runnable onSuccess) {
        executeAsync(
                eng -> eng.updateConfiguration(scope, changes),
                msg -> {
                    context.showToast("Paramètres mis à jour.", AppContext.ToastType.SUCCESS);
                    if (onSuccess != null) onSuccess.run();
                },
                null
        );
    }

    public void createAcademicYear(AcademicYearDraft draft, Consumer<AcademicYearId> onSuccess) {
        worker.submit(() -> {
            try {
                AcademicYearId id = engine.createAcademicYear(draft);
                Platform.runLater(() -> {
                    context.showToast("Année créée avec succès !", AppContext.ToastType.SUCCESS);
                    loadInitialDataSynchronously();
                    if (onSuccess != null) onSuccess.accept(id);
                });
            } catch (Exception e) {
                Platform.runLater(() -> context.showToast(e.getMessage(), AppContext.ToastType.ERROR));
            }
        });
    }

    public void deleteAcademicYear(AcademicYearId yearId, Runnable onSuccess) {
        worker.submit(() -> {
            try {
                engine.deleteAcademicYear(yearId);
                Platform.runLater(() -> {
                    context.showToast("Année supprimée.", AppContext.ToastType.INFO);
                    loadInitialDataSynchronously();
                    if (onSuccess != null) onSuccess.run();
                });
            } catch (Exception e) {
                Platform.runLater(() -> context.showToast(e.getMessage(), AppContext.ToastType.ERROR));
            }
        });
    }

    public void createSemester(SemesterDraft draft, Consumer<SemesterId> onSuccess) {
        worker.submit(() -> {
            try {
                SemesterId id = engine.createSemester(draft);
                Platform.runLater(() -> {
                    context.showToast("Semestre créé avec succès !", AppContext.ToastType.SUCCESS);
                    loadInitialDataSynchronously();
                    if (onSuccess != null) onSuccess.accept(id);
                });
            } catch (Exception e) {
                Platform.runLater(() -> context.showToast(e.getMessage(), AppContext.ToastType.ERROR));
            }
        });
    }

    public void deleteSemester(SemesterId semesterId, Runnable onSuccess) {
        worker.submit(() -> {
            try {
                engine.deleteSemester(semesterId);
                Platform.runLater(() -> {
                    context.showToast("Semestre supprimé.", AppContext.ToastType.INFO);
                    loadInitialDataSynchronously();
                    if (onSuccess != null) onSuccess.run();
                });
            } catch (Exception e) {
                Platform.runLater(() -> context.showToast(e.getMessage(), AppContext.ToastType.ERROR));
            }
        });
    }

    public void createUe(UeDraft draft, Consumer<UeId> onSuccess) {
        worker.submit(() -> {
            try {
                UeId id = engine.createUe(draft);
                Platform.runLater(() -> {
                    context.showToast("UE ajoutée avec succès !", AppContext.ToastType.SUCCESS);
                    refreshActiveSemester(() -> {
                        if (onSuccess != null) onSuccess.accept(id);
                    });
                });
            } catch (Exception e) {
                Platform.runLater(() -> context.showToast(e.getMessage(), AppContext.ToastType.ERROR));
            }
        });
    }

    public void updateUe(UeId ueId, UeDraft draft, Runnable onSuccess) {
        worker.submit(() -> {
            try {
                engine.updateUe(ueId, draft);
                Platform.runLater(() -> {
                    context.showToast("UE mise à jour.", AppContext.ToastType.SUCCESS);
                    refreshActiveSemester(() -> {
                        if (onSuccess != null) onSuccess.run();
                    });
                });
            } catch (Exception e) {
                Platform.runLater(() -> context.showToast(e.getMessage(), AppContext.ToastType.ERROR));
            }
        });
    }

    public void deleteUe(UeId ueId, Runnable onSuccess) {
        worker.submit(() -> {
            try {
                engine.deleteUe(ueId);
                Platform.runLater(() -> {
                    context.showToast("UE supprimée.", AppContext.ToastType.INFO);
                    refreshActiveSemester(() -> {
                        if (onSuccess != null) onSuccess.run();
                    });
                });
            } catch (Exception e) {
                Platform.runLater(() -> context.showToast(e.getMessage(), AppContext.ToastType.ERROR));
            }
        });
    }

    public void syncCurriculumFromYaml(Runnable onComplete) {
        worker.submit(() -> {
            try {
                infrastructure.persistence.CurriculumYamlService yamlService =
                        new infrastructure.persistence.CurriculumYamlService(infrastructure.persistence.JpaManager.getInstance());
                if (yamlService.yamlFileExists()) {
                    boolean imported = yamlService.importFromYaml(yamlService.getYamlFile());
                    Platform.runLater(() -> {
                        if (imported) {
                            context.showToast("Cursus synchronisé depuis cursus.yaml !", AppContext.ToastType.SUCCESS);
                        } else {
                            context.showToast("Impossible de synchroniser cursus.yaml (format invalide).", AppContext.ToastType.WARNING);
                        }
                        loadInitialDataSynchronously();
                        if (onComplete != null) onComplete.run();
                    });
                } else {
                    Platform.runLater(() -> {
                        context.showToast("Aucun fichier ~/.manote/cursus.yaml trouvé.", AppContext.ToastType.INFO);
                        if (onComplete != null) onComplete.run();
                    });
                }
            } catch (Exception e) {
                Platform.runLater(() -> context.showToast("Erreur YAML : " + e.getMessage(), AppContext.ToastType.ERROR));
            }
        });
    }

    public void exportCurriculumToYaml(Runnable onComplete) {
        worker.submit(() -> {
            try {
                infrastructure.persistence.CurriculumYamlService yamlService =
                        new infrastructure.persistence.CurriculumYamlService(infrastructure.persistence.JpaManager.getInstance());
                boolean exported = yamlService.exportToYaml(yamlService.getYamlFile());
                Platform.runLater(() -> {
                    if (exported) {
                        context.showToast("Cursus exporté dans ~/.manote/cursus.yaml", AppContext.ToastType.SUCCESS);
                    } else {
                        context.showToast("Aucune donnée à exporter.", AppContext.ToastType.WARNING);
                    }
                    if (onComplete != null) onComplete.run();
                });
            } catch (Exception e) {
                Platform.runLater(() -> context.showToast("Erreur d'export : " + e.getMessage(), AppContext.ToastType.ERROR));
            }
        });
    }

    public void shutdown() {
        worker.shutdown();
    }

    @FunctionalInterface
    public interface EngineTask {
        void run(GradeEngine engine) throws Exception;
    }
}
