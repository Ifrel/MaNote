package application.engine;

import application.engine.config.ConfigValue;
import application.engine.config.ConfigurationScope;
import application.engine.config.ConfigurationView;
import application.engine.error.EngineException;
import application.engine.error.NotFoundException;
import application.engine.error.OperationNotSupportedException;
import application.engine.error.ValidationException;
import application.engine.model.*;

import java.util.List;
import java.util.Map;

/**
 * <h2>Contrat entre l'interface graphique et le moteur de calcul des notes.</h2>
 *
 * <p>Cette interface est la <strong>seule frontière</strong> entre la présentation ({@code api.*}) et le moteur
 * métier. Les contrôleurs ne connaissent que ce contrat ; l'implémentation est injectée au démarrage
 * (voir {@code infrastructure.engine.EngineLoader}).
 *
 * <h3>Responsabilités</h3>
 * <ul>
 *     <li>Le moteur est la <strong>source de vérité</strong> des données et de tous les calculs : moyennes,
 *     conversions de barème, contributions, validation, simulation. L'interface n'en recalcule aucun.</li>
 *     <li>Une note manquante n'est jamais assimilée à zéro par le contrat : voir {@link GradeEntry} et
 *     {@link application.engine.model.EvaluationStatus}. Son traitement dans les calculs relève du moteur.</li>
 *     <li>Un résultat peut ne pas être calculable : voir {@link application.engine.model.ComputedResult}.</li>
 * </ul>
 *
 * <h3>Exécution</h3>
 * <ul>
 *     <li>Les méthodes sont <strong>synchrones</strong>. L'interface les appelle toujours depuis un unique thread de
 *     travail dédié (jamais depuis le thread JavaFX) : les appels sont sérialisés, l'implémentation n'a donc pas
 *     besoin d'être thread-safe et peut effectuer des entrées/sorties (base de données, fichiers).</li>
 *     <li>L'implémentation ne doit dépendre d'aucune classe JavaFX.</li>
 *     <li>Les objets retournés sont immuables ; l'interface relit les données après chaque modification.</li>
 * </ul>
 *
 * <h3>Erreurs</h3>
 * <ul>
 *     <li>{@link NotFoundException} : identifiant inconnu ;</li>
 *     <li>{@link ValidationException} : données invalides, avec erreurs rattachées aux champs ;</li>
 *     <li>{@link OperationNotSupportedException} : fonctionnalité facultative non prise en charge ;</li>
 *     <li>{@link EngineException} : toute autre erreur métier. Les messages sont affichés à l'utilisateur.</li>
 * </ul>
 *
 * <h3>Fonctionnalités facultatives</h3>
 * Les méthodes signalées « facultative » ont une implémentation par défaut qui lève
 * {@link OperationNotSupportedException}. Elles ne sont appelées que si {@link #capabilities()} les déclare.
 * Un moteur minimal implémente donc uniquement les lectures, la gestion des évaluations et des notes.
 */
public interface GradeEngine {

    // =====================================================================================
    // Métadonnées
    // =====================================================================================

    /**
     * @return description du moteur (nom, version, indicateur de démonstration)
     */
    EngineInfo info();

    /**
     * @return fonctionnalités facultatives prises en charge
     */
    EngineCapabilities capabilities();

    // =====================================================================================
    // Structure académique — lecture
    // =====================================================================================

    /**
     * @return toutes les années universitaires, de la plus récente à la plus ancienne ;
     * au plus une année est marquée {@code current}
     */
    List<AcademicYearSummary> getAcademicYears();

    /**
     * @param yearId année universitaire
     * @return semestres de l'année, triés par numéro
     * @throws NotFoundException si l'année n'existe pas
     */
    List<SemesterSummary> getSemesters(AcademicYearId yearId);

    /**
     * @return le semestre demandé, avec sa moyenne, sa validation et sa progression
     * @throws NotFoundException si le semestre n'existe pas
     */
    SemesterSummary getSemester(SemesterId semesterId);

    /**
     * @return UE du semestre, dans l'ordre d'affichage souhaité
     * @throws NotFoundException si le semestre n'existe pas
     */
    List<UeSummary> getUes(SemesterId semesterId);

    /**
     * @return détail de l'UE : évaluations, contributions (si fournies), explications de calcul
     * @throws NotFoundException si l'UE n'existe pas
     */
    UeDetails getUe(UeId ueId);

    /**
     * @return types d'évaluation disponibles (quiz, TP, projet, examen…)
     */
    List<EvaluationType> getEvaluationTypes();

    // =====================================================================================
    // Évaluations et notes
    // =====================================================================================

    /**
     * Recherche des évaluations. {@link EvaluationFilter#matches} peut être utilisé par l'implémentation.
     *
     * @return évaluations correspondant au filtre, dans un ordre quelconque (l'interface trie)
     */
    List<EvaluationSummary> getEvaluations(EvaluationFilter filter);

    /**
     * @return détail de l'évaluation
     * @throws NotFoundException si l'évaluation n'existe pas
     */
    EvaluationDetails getEvaluation(EvaluationId evaluationId);

    /**
     * Crée une évaluation <em>sans résultat</em> (statut {@code PLANNED}, {@code AWAITING_GRADE} ou {@code CANCELLED}).
     *
     * @return identifiant attribué
     * @throws ValidationException si les données sont invalides (champs : {@code title}, {@code ueId},
     *                             {@code typeCode}, {@code date}, {@code status}, {@code maxPoints}, {@code weight})
     */
    EvaluationId createEvaluation(EvaluationDraft draft);

    /**
     * Modifie une évaluation. Le statut {@code GRADED} n'est accepté que si l'évaluation possède déjà un résultat ;
     * passer d'une évaluation notée à un autre statut est refusé (il faut d'abord {@link #clearGrade retirer la note}).
     *
     * @throws NotFoundException   si l'évaluation n'existe pas
     * @throws ValidationException si les données sont invalides
     */
    void updateEvaluation(EvaluationId evaluationId, EvaluationDraft draft);

    /**
     * Supprime une évaluation et son éventuel résultat.
     *
     * @throws NotFoundException si l'évaluation n'existe pas
     */
    void deleteEvaluation(EvaluationId evaluationId);

    /**
     * Enregistre ou remplace le résultat d'une évaluation ; son statut devient {@code GRADED}.
     * Les éventuelles conversions de barème sont du ressort du moteur.
     *
     * @throws NotFoundException   si l'évaluation n'existe pas
     * @throws ValidationException si le résultat est invalide (champ {@code grade}) ou si l'évaluation est annulée
     */
    void recordGrade(EvaluationId evaluationId, GradeEntry grade);

    /**
     * Retire le résultat d'une évaluation ; son statut redevient {@code AWAITING_GRADE}.
     *
     * @throws NotFoundException si l'évaluation n'existe pas
     */
    void clearGrade(EvaluationId evaluationId);

    // =====================================================================================
    // Structure académique — modification (facultative : capabilities().structureEditing())
    // =====================================================================================

    /**
     * Facultative. Crée une année universitaire.
     */
    default AcademicYearId createAcademicYear(AcademicYearDraft draft) {
        throw new OperationNotSupportedException("création d'année universitaire");
    }

    /**
     * Facultative. Modifie une année universitaire.
     */
    default void updateAcademicYear(AcademicYearId yearId, AcademicYearDraft draft) {
        throw new OperationNotSupportedException("modification d'année universitaire");
    }

    /**
     * Facultative. Supprime une année universitaire.
     */
    default void deleteAcademicYear(AcademicYearId yearId) {
        throw new OperationNotSupportedException("suppression d'année universitaire");
    }

    /**
     * Facultative. Crée un semestre.
     */
    default SemesterId createSemester(SemesterDraft draft) {
        throw new OperationNotSupportedException("création de semestre");
    }

    /**
     * Facultative. Modifie un semestre.
     */
    default void updateSemester(SemesterId semesterId, SemesterDraft draft) {
        throw new OperationNotSupportedException("modification de semestre");
    }

    /**
     * Facultative. Supprime un semestre.
     */
    default void deleteSemester(SemesterId semesterId) {
        throw new OperationNotSupportedException("suppression de semestre");
    }

    /**
     * Facultative. Crée une UE.
     */
    default UeId createUe(UeDraft draft) {
        throw new OperationNotSupportedException("création d'UE");
    }

    /**
     * Facultative. Modifie une UE.
     */
    default void updateUe(UeId ueId, UeDraft draft) {
        throw new OperationNotSupportedException("modification d'UE");
    }

    /**
     * Facultative. Supprime une UE et toutes ses évaluations.
     */
    default void deleteUe(UeId ueId) {
        throw new OperationNotSupportedException("suppression d'UE");
    }

    // =====================================================================================
    // Analyse (facultatives)
    // =====================================================================================

    /**
     * Facultative ({@code capabilities().averageTimeline()}).
     *
     * @return évolution de la moyenne du semestre au fil des résultats, triée par date
     */
    default List<TimelinePoint> getAverageTimeline(SemesterId semesterId) {
        throw new OperationNotSupportedException("évolution de la moyenne");
    }

    /**
     * Facultative ({@code capabilities().simulation()}). Calcule des résultats <em>hypothétiques</em> ;
     * ne modifie aucune donnée. Les valeurs retournées portent la nature {@code SIMULATED}.
     */
    default SimulationResult simulate(SimulationRequest request) {
        throw new OperationNotSupportedException("simulation");
    }

    // =====================================================================================
    // Configuration du calcul (facultative : capabilities().configurationScopes())
    // =====================================================================================

    /**
     * Facultative. Décrit les paramètres de calcul applicables au périmètre demandé.
     *
     * @throws NotFoundException si le périmètre référence une entité inconnue
     */
    default ConfigurationView getConfiguration(ConfigurationScope scope) {
        throw new OperationNotSupportedException("configuration du calcul");
    }

    /**
     * Facultative. Modifie des paramètres dans un périmètre. Seules les clés présentes sont modifiées.
     *
     * @param changes nouvelles valeurs, par clé de paramètre
     * @throws ValidationException avec des erreurs rattachées aux clés des paramètres concernés
     */
    default void updateConfiguration(ConfigurationScope scope, Map<String, ConfigValue> changes) {
        throw new OperationNotSupportedException("configuration du calcul");
    }

    /**
     * Facultative. Supprime les valeurs propres au périmètre afin qu'il hérite à nouveau des valeurs parentes.
     */
    default void resetConfiguration(ConfigurationScope scope) {
        throw new OperationNotSupportedException("réinitialisation de la configuration");
    }
}
